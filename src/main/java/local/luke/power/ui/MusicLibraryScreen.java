package local.luke.power.ui;

import java.nio.file.Path;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.Setting;
import local.luke.power.config.backend.AudioBackend;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.input.Mouse;

/** Embedded Audio panel. Edits belong to the surrounding Options session. */
public final class MusicLibraryScreen extends UiScreen {
  public record State(
      boolean queue, String folder, String query, int trackScroll, int queueScroll, String lastFolder) {
    public State(boolean queue, String folder, String query, int trackScroll, int queueScroll) {
      this(queue, folder, query, trackScroll, queueScroll, "");
    }
  }

  private final PowerOptionsScreen parent;
  private final TextInput search = new TextInput("", 128);
  private final Map<String, Setting> settings = new HashMap<>();
  private List<String> tracks = List.of(), folders = List.of();
  private final Map<String, MusicLibrary.Track> custom = new HashMap<>();
  private final ScrollBar scrollbar = new ScrollBar();
  private int conversionRevision, conversionTicks, trackScroll, queueScroll;
  private boolean queue;
  private String folder = "active", lastFolder = "", error = "", hoverHelp = "";
  private Set<String> activeTracks = Set.of();

  public boolean trackStatusVisible() { return error.isEmpty() && !Mp3Converter.busy() && conversionTicks == 0; }

  public String hoverHelp() {
    return hoverHelp;
  }

  private int revision = -1;
  private Setting dragging;
  private List<String> renderedQueue = List.of();

  public MusicLibraryScreen(PowerOptionsScreen parent) {
    this.parent = parent;
  }

  public State state() {
    return new State(queue, folder, search.text(), trackScroll, queueScroll, lastFolder);
  }

  public void restore(State state) {
    if (state == null) return;
    queue = state.queue;
    folder = state.folder;
    lastFolder = state.lastFolder;
    search.setText(state.query);
    trackScroll = state.trackScroll;
    queueScroll = state.queueScroll;
    search.focused = false;
  }

  private int left() {
    return parent.contentLeft();
  }

  private int right() {
    return parent.contentRight();
  }

  private int span() {
    return right() - left();
  }

  private int bottom() {
    return parent.contentBottom();
  }

  private int listTop() {
    return controlsTop() + (queue ? 18 : filters().height()) + 6;
  }

  private int statusTop() { return parent.audioContentTop(); }
  private int controlsTop() { return statusTop() + 15; }

  private boolean narrow() {
    return span() < 350;
  }

  private int rowHeight() {
    return 44;
  }

  private int queueWidth(int action) {
    return switch (action) {
      case 0, 1 -> narrow() ? 14 : 20;
      case 2 -> narrow() ? 36 : 48;
      case 3 -> narrow() ? 42 : 54;
      default -> throw new IllegalArgumentException("Unknown queue action");
    };
  }

  private int queueX(int action) {
    int x = right();
    for (int i = 3; i >= action; i--) x -= queueWidth(i) + (i == 3 ? 0 : 2);
    return x;
  }

  private int filterX() {
    return queue ? right() - 82 : left();
  }

  private int filterWidth() { return queue ? 82 : Math.min(240, span()); }
  public boolean queueVisible() { return queue; }

  private void show(boolean queue) {
    dragging = null;
    scrollbar.release();
    parent.finishContinuousChange();
    search.focused = false;
    this.queue = queue;
    error = "";
    rebuild();
  }

  public void showQueue() { show(true); }
  public void showTracks() { show(false); }

  public void showTrack(String id) {
    show(false);
    folder = "";
    search.setText("");
    rebuild();
    int index = tracks.indexOf(id);
    if (index >= 0) scroll(index * rowHeight());
  }

  private boolean folderSelected() {
    return !folder.equals("active") && !folder.isEmpty() && !folder.equals("custom");
  }

  private MusicFilters.Layout filters() {
    return MusicFilters.layout(left(), controlsTop(), span(), folderSelected());
  }

  private void selectFilter(String id) {
    folder = id.equals("folder")
        ? folders.contains(lastFolder) ? lastFolder : folders.isEmpty() ? "folder" : folders.get(0)
        : id;
    if (folderSelected()) lastFolder = folder;
    scroll(0);
    rebuild();
  }

  private int scroll() {
    return queue ? queueScroll : trackScroll;
  }

  private void scroll(int value) {
    int bounded =
        Math.max(
            0, Math.min(value, Math.max(0, tracks.size() * rowHeight() - (bottom() - listTop()))));
    if (queue) queueScroll = bounded;
    else trackScroll = bounded;
  }

  public void top() {
    scroll(0);
  }

  private int volumeX() {
    return left() + 64;
  }

  private int volumeEnd() {
    return right() - (queue || narrow() ? 26 : 144);
  }

  private int actionY(int rowY) {
    return rowY + (narrow() ? 0 : 19);
  }

  private ScrollBar.Track track() {
    return new ScrollBar.Track(
        right() + 5, listTop(), bottom() - listTop(), tracks.size() * rowHeight(), scroll());
  }

  public boolean hasQuery() {
    return !queue && !search.text().isEmpty();
  }

  public boolean focused() {
    return search.focused;
  }

  public void clearQuery() {
    search.setText("");
    scroll(0);
    rebuild();
  }

  public void unfocus() {
    search.focused = false;
  }

  public void focus() {
    if (queue) showTracks();
    search.focused = true;
    search.selectAll();
  }

  public void init() {
    refresh();
  }

  public void removed() {
    dragging = null;
    scrollbar.release();
    parent.finishContinuousChange();
  }

  private void click() {
    minecraft.soundManager.method_2009("random.click", 1, 1);
  }

  private void refresh() {
    AudioBackend.discoverMusic(parent.session(), minecraft);
    custom.clear();
    AudioController.customTracks().forEach(t -> custom.put(t.id(), t));
    TreeSet<String> paths = new TreeSet<>(Comparator.comparing((String path) -> path.toLowerCase(Locale.ROOT)).thenComparing(Comparator.naturalOrder()));
    for (List<String> roots : List.of(AudioConfig.current().musicDirectories, AudioConfig.current().menuDirectories)) {
      for (String root : roots) {
        Path resolved = MusicLibrary.resolve(FabricLoader.getInstance().getGameDir(), root).toAbsolutePath().normalize();
        try { resolved = resolved.toRealPath(); } catch (java.io.IOException ignored) { /* Keep unavailable folders selectable. */ }
        paths.add(resolved.toString());
      }
    }
    for (var t : custom.values())
      if (t.path().getParent() != null) paths.add(t.path().getParent().toString());
    folders = List.copyOf(paths);
    settings.clear();
    parent.session().settings().forEach(s -> settings.put(s.id, s));
    revision = AudioController.libraryRevision();
    rebuild();
  }

  private void rebuild() {
    activeTracks = AudioController.activeMusic(minecraft);
    List<String> source =
        queue ? MusicRequests.tracks() : new ArrayList<>(AudioController.music(minecraft));
    Map<String, Integer> scores = new HashMap<>();
    if (!queue)
      for (String id : source)
        scores.put(id, FuzzySearch.score(search.text(), AudioController.musicLabel(id), path(id)));
    tracks =
        queue
            ? source
            : source.stream()
                .filter(this::inFolder)
                .filter(id -> scores.get(id) < FuzzySearch.NONE)
                .sorted(
                    Comparator.comparingInt((String id) -> scores.get(id))
                        .thenComparing(AudioController::musicLabel, String.CASE_INSENSITIVE_ORDER))
                .toList();
    scroll(scroll());
  }

  private boolean inFolder(String id) {
    if (folder.equals("active")) return activeTracks.contains(id);
    if (folder.isEmpty()) return true;
    MusicLibrary.Track t = custom.get(id);
    if (folder.equals("custom")) return t != null;
    return !folder.equals("folder") && t != null
        && t.path().getParent() != null
        && t.path().startsWith(Path.of(folder));
  }

  private String path(String id) {
    MusicLibrary.Track t = custom.get(id);
    return t == null ? "Built-in soundtrack" : t.path().toString();
  }

  private boolean playable(String id) {
    MusicLibrary.Track t = custom.get(id);
    return t == null ? !id.startsWith("music:custom/") : t.playable();
  }

  private String folderLabel() {
    return folders.isEmpty() ? "No folders" : Objects.toString(Path.of(folder).getFileName(), folder);
  }

  private void cycleFolder(int direction) {
    if (folders.isEmpty()) return;
    folder = folders.get(Math.floorMod(folders.indexOf(folder) + direction, folders.size()));
    lastFolder = folder;
    scroll(0);
    rebuild();
  }

  public void tick() {
    if (conversionRevision != Mp3Converter.revision()) {
      conversionRevision = Mp3Converter.revision();
      conversionTicks = 200;
    } else if (conversionTicks > 0) conversionTicks--;
    if (revision != AudioController.libraryRevision()) refresh();
    else if (queue && !tracks.equals(MusicRequests.tracks())) rebuild();
    else if (!queue && folder.equals("active") && activeTracks != AudioController.activeMusic(minecraft)) rebuild();
  }

  public void wheel(int amount) {
    if (amount != 0) scroll(scroll() - (int) Math.signum(amount) * rowHeight());
  }

  private void slide(int x) {
    if (dragging == null) return;
    var before = dragging.value;
    dragging.slide((x - volumeX() - 4d) / Math.max(1, volumeEnd() - volumeX() - 8));
    if (!before.equals(dragging.value)) parent.changed(dragging, true);
  }

  private boolean queueEdit(java.util.function.Consumer<MusicQueue> change) {
    // Compare the list the user actually saw, not a newer tick's list. Duplicates are intentional.
    if (queue && !renderedQueue.equals(MusicRequests.tracks())) {
      rebuild();
      error = "Queue changed. Select the track again.";
      return false;
    }
    try {
      MusicRequests.edit(change);
      error = "";
      rebuild();
      return true;
    } catch (Exception e) {
      error = Objects.toString(e.getMessage(), "Could not save queue");
      return false;
    }
  }

  public void searchClick(int x, int y, int button) {
    if (queue) return;
    search.focused = true;
    if (button == 1) clearQuery();
  }

  protected void keyPressed(char c, int key) {
    if (!queue && search.focused) {
      search.key(c, key);
      scroll(0);
      rebuild();
    }
  }

  protected void mouseClicked(int x, int y, int b) {
    if (b != 0 && b != 1) return;
    if (b == 0 && scrollbar.press(track(), x, y)) {
      scroll((int) scrollbar.drag(track(), y, true));
      return;
    }
    search.focused = false;
    if (queue && inside(x, y, filterX(), controlsTop(), filterWidth(), 18)) {
      if (!tracks.isEmpty()) { click(); queueEdit(q -> q.tracks.clear()); }
      return;
    }
    if (!queue) {
      var tabs = filters();
      for (var tab : tabs.tabs()) if (inside(x, y, tab.x(), tab.y(), tab.width(), 18)) {
        click(); selectFilter(tab.id()); return;
      }
      if (folderSelected() && inside(x, y, tabs.folderX(), tabs.folderY(), tabs.folderWidth(), 18)) {
        if (!folders.isEmpty()) { click(); cycleFolder(b == 0 ? 1 : -1); }
        return;
      }
    }
    if (!queue && inside(x, y, right() - 88, statusTop(), 88, 14)) {
      click();
      if (Mp3Converter.busy()) Mp3Converter.cancel();
      else
        Mp3Converter.start(FabricLoader.getInstance().getGameDir(), AudioController.customTracks());
      return;
    }
    if (y < listTop() || y >= bottom() || x < left() || x >= right()) return;
    int index = (y - listTop() + scroll()) / rowHeight();
    if (index < 0 || index >= tracks.size()) return;
    if (queue && !renderedQueue.equals(MusicRequests.tracks())) {
      rebuild();
      error = "Queue changed. Select the track again.";
      return;
    }
    String id = tracks.get(index);
    int rowY = listTop() + index * rowHeight() - scroll();
    Setting volume = settings.get("audio.sound." + id);
    int volumeY = rowY + (queue ? 24 : 19);
    if (volume != null && inside(x, y, volumeX(), volumeY, volumeEnd() - volumeX(), 18) && b == 0) {
      dragging = volume; slide(x); return;
    }
    if (volume != null && inside(x, y, volumeEnd() + 4, volumeY, 22, 18)) {
      click(); minecraft.setScreen(new ValueScreen(parent, volume)); return;
    }
    if (queue) {
      if (inside(x, y, queueX(3), rowY + 3, queueWidth(3), 18)) {
        click();
        queueEdit(q -> q.tracks.remove(index));
      } else if (inside(x, y, queueX(2), rowY + 3, queueWidth(2), 18) && playable(id)) {
        click();
        AudioController.toggleTrack(id);
      } else if (inside(x, y, queueX(0), rowY + 3, queueWidth(0), 18) && index > 0) {
        click();
        queueEdit(q -> q.move(index, -1));
      } else if (inside(x, y, queueX(1), rowY + 3, queueWidth(1), 18) && index + 1 < tracks.size()) {
        click();
        queueEdit(q -> q.move(index, 1));
      }
      return;
    }
    Setting enabled = settings.get("audio.trackEnabled." + id);
    if (inside(x, y, left(), rowY + 19, 60, 18) && enabled != null) {
      click();
      enabled.cycle(1);
      parent.changed(enabled);
    } else if (inside(x, y, right() - 116, actionY(rowY), 20, 18) && playable(id)) {
      click();
      AudioController.previewSound(id);
    } else if (inside(x, y, right() - 92, actionY(rowY), 42, 18) && playable(id)) {
      click();
      AudioController.toggleTrack(id);
    } else if (inside(x, y, right() - 46, actionY(rowY), 46, 18)) {
      click();
      queueEdit(q -> q.add(id));
    }
  }

  public void renderSearch(int x, int y, int w, int mx, int my) {
    if (queue) {
      text("Music queue", x + 2, y + 5, 0xdddddd);
    } else input(search, x, y, w, mx, my, "Search tracks or folders...");
  }

  public void render(int mx, int my, float delta) {
    scroll((int) scrollbar.drag(track(), my, Mouse.isButtonDown(0)));
    if (dragging != null) {
      if (Mouse.isButtonDown(0)) slide(mx);
      else {
        dragging = null;
        parent.finishContinuousChange();
      }
    }
    String tip = "";
    if (queue) {
      text("Up next", left() + 2, controlsTop() + 5, 0xdddddd);
      button("Clear queue", filterX(), controlsTop(), filterWidth(), 18, mx, my, !tracks.isEmpty());
      if (inside(mx, my, filterX(), controlsTop(), filterWidth(), 18))
        tip = "Remove queued requests. Music files and rotation stay unchanged.";
    } else {
      var tabs = filters();
      for (var tab : tabs.tabs()) {
        boolean selected = tab.id().equals("folder") ? folderSelected() : tab.id().equals(folder);
        fill(tab.x(), tab.y(), tab.x() + tab.width(), tab.y() + 18, selected ? 0xb0000000 : 0x40000000);
        text(tab.label(), tab.x() + 4, tab.y() + 5, selected ? 0xffffff : 0xaaaaaa);
        if (selected) fill(tab.x(), tab.y() + 17, tab.x() + tab.width(), tab.y() + 18, 0xffaaaaaa);
        if (inside(mx, my, tab.x(), tab.y(), tab.width(), 18))
          tip = switch (tab.id()) {
            case "active" -> "The pool allowed by your soundtrack, folders and world/menu rules. Excluded tracks remain visible.";
            case "" -> "All available built-in and custom music.";
            case "custom" -> "All tracks found in your music folders.";
            default -> "Tracks in the selected folder and its subfolders.";
          };
      }
      if (folderSelected()) {
        button(fit(folderLabel(), tabs.folderWidth() - 8), tabs.folderX(), tabs.folderY(), tabs.folderWidth(), 18, mx, my, !folders.isEmpty());
        if (inside(mx, my, tabs.folderX(), tabs.folderY(), tabs.folderWidth(), 18))
          tip = folders.isEmpty() ? "Add music folders in Audio settings." : folder;
      }
    }
    if (queue) renderedQueue = List.copyOf(tracks);
    clip(left() - 2, listTop(), span() + 4, Math.max(0, bottom() - listTop()));
    for (int i = 0; i < tracks.size(); i++) {
      int y = listTop() + i * rowHeight() - scroll();
      if (y + rowHeight() <= listTop() || y >= bottom()) continue;
      String id = tracks.get(i);
      boolean hover =
          inside(mx, my, left(), y, span(), rowHeight()) && my >= listTop() && my < bottom();
      if (hover) fill(left() - 2, y, right() + 2, y + rowHeight() - 1, 0x60000000);
      text(
          fit((queue ? (i + 1) + ". " : "") + AudioController.musicLabel(id), queue ? queueX(0) - left() - 8 : span() - (narrow() ? 122 : 4)),
          left() + 2,
          y + (queue ? 8 : 4),
          playable(id) ? 0xdddddd : 0xffbb77);
      if (hover)
        tip = path(id) + (playable(id) ? "" : "\nConvert MP3, or reload if the file has moved.");
      if (queue) {
        iconButton("up", queueX(0), y + 3, queueWidth(0), mx, my, i > 0);
        iconButton("down", queueX(1), y + 3, queueWidth(1), mx, my, i + 1 < tracks.size());
        button(
            AudioController.playingTrack(id) ? "Pause" : "Play",
            queueX(2),
            y + 3,
            queueWidth(2),
            18,
            mx,
            my,
            playable(id));
        button("Remove", queueX(3), y + 3, queueWidth(3), 18, mx, my, true);
        if (hover && my >= y + 3 && my < y + 21)
          tip =
              mx >= queueX(3)
                  ? "Remove this request"
                  : mx >= queueX(2)
                      ? "Play or pause this track"
                      : mx >= queueX(1) ? "Move down" : mx >= queueX(0) ? "Move up" : tip;
        Setting volume = settings.get("audio.sound." + id);
        text("Volume", left() + 2, y + 29, 0xaaaaaa);
        if (volume != null) slider(volume.display() + "%", volumeX(), y + 24,
            volumeEnd() - volumeX(), mx, my, volume.value.getAsDouble() / 100);
        button("...", volumeEnd() + 4, y + 24, 22, 18, mx, my, volume != null);
        if (hover && my >= y + 24) tip = "Track volume; music and master volumes also apply.";
      } else {
        Setting enabled = settings.get("audio.trackEnabled." + id),
            volume = settings.get("audio.sound." + id);
        button(
            enabled != null && enabled.value.getAsBoolean() ? "Included" : "Excluded",
            left(),
            y + 19,
            60,
            18,
            mx,
            my,
            enabled != null);
        if (volume != null)
          slider(
              volume.display() + "%",
              volumeX(),
              y + 19,
              volumeEnd() - volumeX(),
              mx,
              my,
              volume.value.getAsDouble() / 100);
        button("...", volumeEnd() + 4, y + 19, 22, 18, mx, my, volume != null);
        iconButton(
            "speaker",
            right() - 116,
            actionY(y),
            20,
            mx,
            my,
            playable(id),
            AudioController.previewing(id));
        button(
            AudioController.playingTrack(id) ? "Pause" : "Play",
            right() - 92,
            actionY(y),
            42,
            18,
            mx,
            my,
            playable(id));
        button("Queue", right() - 46, actionY(y), 46, 18, mx, my, true);
        if (hover) {
          if (inside(mx, my, left(), y + 19, 60, 18))
            tip = "Include in automatic rotation. Preview and queue work even when excluded.";
          else if (inside(mx, my, volumeX(), y + 19, volumeEnd() - volumeX(), 18))
            tip = "Track volume; music and master volumes also apply.";
          else if (inside(mx, my, volumeEnd() + 4, y + 19, 22, 18)) tip = "Enter an exact volume";
          else if (my >= actionY(y) && my < actionY(y) + 18 && mx >= right() - 116)
            tip =
                mx < right() - 96
                    ? "Preview without changing the current track; click again to stop."
                    : mx < right() - 50
                        ? "Play or pause this track"
                        : "Add to the end of the queue";
        }
      }
    }
    if (tracks.isEmpty())
      text(queue ? "Queue is empty" : "No matching tracks.", left() + 4, listTop() + 12, 0xaaaaaa);
    unclip();
    scrollbar.render(this, track());
    if (!queue) {
      button(Mp3Converter.busy() ? "Cancel MP3" : "Convert MP3", right() - 88, statusTop(), 88, 14, mx, my, true);
      if (inside(mx, my, right() - 88, statusTop(), 88, 14))
        tip = "Convert MP3 with FFmpeg to a WAV cache. Original files stay untouched.";
    }
    String status =
        !error.isEmpty()
            ? error
            : Mp3Converter.busy() || conversionTicks > 0
                ? Mp3Converter.status()
                : AudioController.status();
    text(fit(status, span() - (queue ? 0 : 92)), left(), statusTop() + 4, error.isEmpty() ? 0xaaaaaa : 0xff8888);
    hoverHelp = tip;
  }
}
