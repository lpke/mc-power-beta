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
      boolean queue, String folder, String query, int trackScroll, int queueScroll) {}

  private final PowerOptionsScreen parent;
  private final TextInput search = new TextInput("", 128);
  private final Map<String, Setting> settings = new HashMap<>();
  private List<String> tracks = List.of(), folders = List.of();
  private final Map<String, MusicLibrary.Track> custom = new HashMap<>();
  private final ScrollBar scrollbar = new ScrollBar();
  private int conversionRevision, conversionTicks, trackScroll, queueScroll;
  private boolean queue;
  private String folder = "active", error = "", hoverHelp = "";
  private Set<String> activeTracks = Set.of();

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
    return new State(queue, folder, search.text(), trackScroll, queueScroll);
  }

  public void restore(State state) {
    if (state == null) return;
    queue = state.queue;
    folder = state.folder;
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
    return controlsTop() + 24;
  }

  private int statusTop() { return parent.audioContentTop(); }
  private int controlsTop() { return statusTop() + 15; }

  private boolean narrow() {
    return span() < 350;
  }

  private int rowHeight() {
    return queue ? 24 : 44;
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
    return right() - (narrow() ? 26 : 144);
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
    return t != null
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
    return folder.equals("active") ? "Active tracks" : folder.isEmpty()
        ? "All tracks"
        : folder.equals("custom")
            ? "Custom tracks"
            : Objects.toString(Path.of(folder).getFileName(), folder);
  }

  private void cycleFolder(int direction) {
    List<String> choices = new ArrayList<>(List.of("active", "", "custom"));
    choices.addAll(folders);
    folder = choices.get(Math.floorMod(choices.indexOf(folder) + direction, choices.size()));
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
    if (inside(x, y, filterX(), controlsTop(), filterWidth(), 18)) {
      if (queue && tracks.isEmpty()) return;
      click();
      if (queue) queueEdit(q -> q.tracks.clear());
      else cycleFolder(b == 0 ? 1 : -1);
      return;
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
    String id = tracks.get(index);
    int rowY = listTop() + index * rowHeight() - scroll();
    if (queue) {
      if (!renderedQueue.equals(MusicRequests.tracks())) {
        rebuild();
        error = "Queue changed. Select the track again.";
        return;
      }
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
    Setting enabled = settings.get("audio.trackEnabled." + id),
        volume = settings.get("audio.sound." + id);
    if (inside(x, y, left(), rowY + 19, 60, 18) && enabled != null) {
      click();
      enabled.cycle(1);
      parent.changed(enabled);
    } else if (inside(x, y, volumeX(), rowY + 19, volumeEnd() - volumeX(), 18)
        && volume != null
        && b == 0) {
      dragging = volume;
      slide(x);
    } else if (inside(x, y, volumeEnd() + 4, rowY + 19, 22, 18) && volume != null) {
      click();
      minecraft.setScreen(new ValueScreen(parent, volume));
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
    int filterX = filterX();
    if (queue) text("Up next", left() + 2, controlsTop() + 5, 0xdddddd);
    button(
        fit(queue ? "Clear queue" : folderLabel(), filterWidth() - 8),
        filterX,
        controlsTop(),
        filterWidth(),
        18,
        mx,
        my,
        !queue || !tracks.isEmpty());
    String tip = "";
    if (inside(mx, my, filterX, controlsTop(), filterWidth(), 18))
      tip =
          queue
              ? "Remove queued requests. Music files and rotation stay unchanged."
              : folder.equals("active")
                  ? "Follows soundtrack, enabled folders and current world/menu rules. Individually excluded tracks stay visible."
                  : folder.isEmpty() || folder.equals("custom")
                  ? "Choose active tracks, all tracks, custom tracks, or a folder and its subfolders."
                  : folder;
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
