package local.luke.power.ui;

import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.Setting;
import local.luke.power.config.backend.AudioBackend;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** One library across all configured folders, with a separate ordered queue. */
public final class MusicLibraryScreen extends UiScreen {
  private final PowerOptionsScreen parent;
  private final TextInput search = new TextInput("", 128);
  private final Map<String, Setting> settings = new HashMap<>();
  private List<String> tracks = List.of();
  private final Map<String, MusicLibrary.Track> custom = new HashMap<>();
  private int conversionRevision, conversionTicks;
  private boolean queue, allTracks;
  private int scroll, revision = -1, selected = -1;
  private String error = "";
  private Setting dragging;

  public MusicLibraryScreen(PowerOptionsScreen parent) {
    this.parent = parent;
  }

  private int span() {
    return Math.min(620, Math.min(width, height * 16 / 9) - 24);
  }

  private int left() {
    return (width - span()) / 2;
  }

  private int right() {
    return left() + span();
  }

  private int bottom() {
    return height - 57;
  }

  private int listTop() {
    return 112;
  }

  private int tabWidth() {
    return Math.min(100, span() / 3 - 4);
  }

  private int volumeX() {
    return left() + 76;
  }

  private int volumeWidth() {
    return Math.max(30, span() - 222);
  }

  public void init() {
    Keyboard.enableRepeatEvents(true);
    refresh();
  }

  public void removed() {
    dragging = null;
    parent.finishContinuousChange();
    Keyboard.enableRepeatEvents(false);
  }

  private void click() {
    minecraft.soundManager.method_2009("random.click", 1, 1);
  }

  private void refresh() {
    AudioBackend.discoverMusic(parent.session(), minecraft);
    custom.clear();
    AudioController.customTracks().forEach(t -> custom.put(t.id(), t));
    settings.clear();
    parent.session().settings().forEach(s -> settings.put(s.id, s));
    revision = AudioController.libraryRevision();
    rebuild();
  }

  private void rebuild() {
    List<String> source =
        queue
            ? MusicRequests.tracks()
            : allTracks
                ? new ArrayList<>(AudioController.music(minecraft))
                : AudioController.customTracks().stream().map(MusicLibrary.Track::id).toList();
    Map<String, Integer> scores = new HashMap<>();
    for (String id : source)
      scores.put(id, FuzzySearch.score(search.text(), AudioController.musicLabel(id), path(id)));
    tracks =
        queue
            ? source
            : source.stream()
                .filter(id -> scores.get(id) < FuzzySearch.NONE)
                .sorted(
                    Comparator.comparingInt((String id) -> scores.get(id))
                        .thenComparing(AudioController::musicLabel, String.CASE_INSENSITIVE_ORDER))
                .toList();
    scroll =
        Math.max(0, Math.min(scroll, Math.max(0, tracks.size() * 43 - (bottom() - listTop()))));
    if (selected >= tracks.size()) selected = -1;
  }

  private String path(String id) {
    MusicLibrary.Track track = custom.get(id);
    return track == null ? "Built-in soundtrack" : track.path().toString();
  }

  private boolean playable(String id) {
    MusicLibrary.Track track = custom.get(id);
    return track == null ? !id.startsWith("music:custom/") : track.playable();
  }

  public void tick() {
    if (conversionRevision != Mp3Converter.revision()) {
      conversionRevision = Mp3Converter.revision();
      conversionTicks = 200;
    } else if (conversionTicks > 0) conversionTicks--;
    if (revision != AudioController.libraryRevision()) refresh();
    else if (queue && !tracks.equals(MusicRequests.tracks())) { selected = -1; rebuild(); }
  }

  public void onMouseEvent() {
    super.onMouseEvent();
    int wheel = Mouse.getEventDWheel();
    if (wheel != 0) {
      scroll -= (int) Math.signum(wheel) * 43;
      rebuild();
    }
  }

  private void slide(int x) {
    if (dragging == null) return;
    var before = dragging.value;
    dragging.slide((x - volumeX() - 4d) / Math.max(1, volumeWidth() - 8));
    if (!before.equals(dragging.value)) parent.changed(dragging, true);
  }

  private boolean queueEdit(java.util.function.Consumer<MusicQueue> change) {
    if (queue && !tracks.equals(MusicRequests.tracks())) {
      selected = -1; rebuild(); error = "Queue changed. Select the track again."; return false;
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

  protected void mouseClicked(int x, int y, int b) {
    if (b != 0 && b != 1) return;
    if (inside(x, y, left(), 32, tabWidth(), 20)) {
      click();
      queue = false;
      selected = -1;
      scroll = 0;
      rebuild();
      return;
    }
    if (inside(x, y, left() + tabWidth() + 4, 32, tabWidth(), 20)) {
      click();
      queue = true;
      selected = -1;
      scroll = 0;
      rebuild();
      return;
    }
    if (!queue && inside(x, y, right() - 106, 32, 106, 20)) {
      click();
      allTracks = !allTracks;
      scroll = 0;
      rebuild();
      return;
    }
    search.focused = !queue && inside(x, y, left(), 62, span(), 20);
    if (search.focused) return;
    if (y >= 87 && y < 107 && x >= left() && x < right()) {
      int i = Math.min(3, (x - left()) / Math.max(1, span() / 4));
      click();
      if (queue) {
        if (i == 0) AudioController.playQueue();
        if (i == 1 && selected > 0) {
          final int row = selected;
          if (queueEdit(q -> q.move(row, -1))) selected--;
        }
        if (i == 2 && selected >= 0 && selected + 1 < tracks.size()) {
          final int row = selected;
          if (queueEdit(q -> q.move(row, 1))) selected++;
        }
        if (i == 3) queueEdit(q -> q.tracks.clear());
      } else {
        if (i == 0) AudioController.togglePause();
        if (i == 1) AudioController.previous();
        if (i == 2) AudioController.next();
        if (i == 3) AudioController.reload();
      }
      return;
    }
    if (y >= listTop() && y < bottom() && x >= left() && x < right()) {
      int index = (y - listTop() + scroll) / 43;
      if (index >= tracks.size()) return;
      String id = tracks.get(index);
      selected = index;
      int rowY = listTop() + index * 43 - scroll;
      if (y < rowY + 18) return;
      if (queue) {
        if (inside(x, y, right() - 54, rowY + 19, 54, 18)) {
          click();
          queueEdit(q -> q.tracks.remove(index));
        } else if (inside(x, y, right() - 112, rowY + 19, 54, 18) && playable(id)) {
          click();
          AudioController.playNow(id);
        }
        return;
      }
      Setting enabled = settings.get("audio.trackEnabled." + id),
          volume = settings.get("audio.sound." + id);
      if (inside(x, y, left(), rowY + 19, 70, 18) && enabled != null) {
        click();
        enabled.cycle(1);
        parent.changed(enabled);
      } else if (inside(x, y, volumeX(), rowY + 19, volumeWidth(), 18)
          && volume != null
          && b == 0) {
        dragging = volume;
        slide(x);
      } else if (inside(x, y, right() - 142, rowY + 19, 22, 18) && volume != null) {
        click();
        minecraft.setScreen(new ValueScreen(parent, volume, this));
      } else if (inside(x, y, right() - 116, rowY + 19, 20, 18) && playable(id)) {
        click();
        AudioController.previewSound(id);
      } else if (inside(x, y, right() - 92, rowY + 19, 42, 18) && playable(id)) {
        click();
        AudioController.playNow(id);
      } else if (inside(x, y, right() - 46, rowY + 19, 46, 18)) {
        click();
        queueEdit(q -> q.add(id));
      }
      return;
    }
    if (inside(x, y, left(), height - 28, 112, 20)) {
      click();
      if (Mp3Converter.busy()) Mp3Converter.cancel();
      else
        Mp3Converter.start(FabricLoader.getInstance().getGameDir(), AudioController.customTracks());
    }
    if (inside(x, y, right() - 90, height - 28, 90, 20)) {
      click();
      minecraft.setScreen(parent);
    }
  }

  protected void keyPressed(char c, int key) {
    if (key == Keyboard.KEY_ESCAPE) {
      minecraft.setScreen(parent);
      return;
    }
    if (!queue && search.focused) {
      search.key(c, key);
      scroll = 0;
      rebuild();
    }
  }

  public void render(int mx, int my, float delta) {
    if (dragging != null) {
      if (Mouse.isButtonDown(0)) slide(Mouse.getX() * width / minecraft.displayWidth);
      else {
        dragging = null;
        parent.finishContinuousChange();
      }
    }
    renderBackground();
    drawCenteredTextWithShadow(textRenderer, "Music library", width / 2, 13, 0xffffff);
    button(queue ? "Tracks" : "[ Tracks ]", left(), 32, tabWidth(), 20, mx, my, true);
    button(
        queue ? "[ Queue ]" : "Queue (" + MusicRequests.tracks().size() + ")",
        left() + tabWidth() + 4,
        32,
        tabWidth(),
        20,
        mx,
        my,
        true);
    if (!queue) {
      button(allTracks ? "All tracks" : "Custom tracks", right() - 106, 32, 106, 20, mx, my, true);
      input(search, left(), 63, span(), mx, my, "Search tracks or folders...");
    } else text("Queued tracks play in order, including excluded tracks.", left(), 67, 0xaaaaaa);
    String[] actions =
        queue
            ? new String[] {"Play queue", "Move up", "Move down", "Clear queue"}
            : new String[] {"Play / pause", "Previous", "Next", "Reload folders"};
    for (int i = 0; i < 4; i++)
      button(
          actions[i],
          left() + i * (span() / 4),
          87,
          span() / 4 - 3,
          20,
          mx,
          my,
          !queue
              || i == 0
              || i == 3
              || i == 1 && selected > 0
              || i == 2 && selected >= 0 && selected + 1 < tracks.size());
    String tip = "";
    clip(left() - 2, listTop(), span() + 4, Math.max(0, bottom() - listTop()));
    for (int i = 0; i < tracks.size(); i++) {
      int y = listTop() + i * 43 - scroll;
      if (y + 43 <= listTop() || y >= bottom()) continue;
      String id = tracks.get(i);
      boolean hover = inside(mx, my, left(), y, span(), 43) && my >= listTop() && my < bottom();
      if (hover || queue && selected == i) fill(left() - 2, y, right() + 2, y + 42, 0x60000000);
      text(
          fit((queue ? (i + 1) + ". " : "") + AudioController.musicLabel(id), span() - 4),
          left() + 2,
          y + 4,
          playable(id) ? 0xffffff : 0xffbb77);
      if (hover)
        tip =
            path(id) + (playable(id) ? "" : "\nMP3 needs conversion, or the file is unavailable.");
      if (queue) {
        text(playable(id) ? "Queued" : "Unavailable", left(), y + 24, 0xaaaaaa);
        button("Play", right() - 112, y + 19, 54, 18, mx, my, playable(id));
        button("Remove", right() - 54, y + 19, 54, 18, mx, my, true);
      } else {
        Setting enabled = settings.get("audio.trackEnabled." + id),
            volume = settings.get("audio.sound." + id);
        button(
            enabled != null && enabled.value.getAsBoolean() ? "Included" : "Excluded",
            left(),
            y + 19,
            70,
            18,
            mx,
            my,
            enabled != null);
        if (volume != null)
          slider(
              volume.display() + "%",
              volumeX(),
              y + 19,
              volumeWidth(),
              mx,
              my,
              volume.value.getAsDouble() / 100);
        button("...", right() - 142, y + 19, 22, 18, mx, my, volume != null);
        iconButton("speaker", right() - 116, y + 19, 20, mx, my, playable(id));
        button("Play", right() - 92, y + 19, 42, 18, mx, my, playable(id));
        button("Queue", right() - 46, y + 19, 46, 18, mx, my, true);
        if (hover && my >= y + 19)
          tip =
              mx < volumeX()
                  ? "Include in automatic rotation. Preview and queue work even when excluded."
                  : mx < right() - 142
                      ? "Track volume; music and master volumes also apply."
                      : mx < right() - 120
                          ? "Enter an exact volume"
                          : mx < right() - 96
                              ? "Preview without changing the current track; click again to stop."
                              : mx < right() - 50
                                  ? "Play immediately"
                                  : "Add to the end of the queue";
      }
    }
    if (tracks.isEmpty())
      text(
          queue ? "Queue is empty" : "No tracks. Add music folders in Audio settings.",
          left() + 4,
          listTop() + 12,
          0xaaaaaa);
    unclip();
    int trackHeight = bottom() - listTop(), total = tracks.size() * 43;
    if (total > trackHeight && trackHeight > 0) {
      int thumb = Math.max(12, trackHeight * trackHeight / total);
      int y = listTop() + scroll * (trackHeight - thumb) / (total - trackHeight);
      fill(right() + 4, listTop(), right() + 7, bottom(), 0xff222222);
      fill(right() + 4, y, right() + 7, y + thumb, 0xff777777);
    }
    String message =
        !error.isEmpty()
            ? error
            : Mp3Converter.busy() || conversionTicks > 0
                ? Mp3Converter.status()
                : AudioController.status();
    text(fit(message, span()), left(), height - 46, error.isEmpty() ? 0xaaaaaa : 0xff8888);
    button(
        Mp3Converter.busy() ? "Cancel conversion" : "Convert MP3...",
        left(),
        height - 28,
        112,
        20,
        mx,
        my,
        true);
    button("Done", right() - 90, height - 28, 90, 20, mx, my, true);
    if (inside(mx, my, left(), height - 28, 112, 20))
      tip =
          "Convert up to 256 MP3 files with FFmpeg into a WAV cache. Original files stay untouched."
              + " Conversion runs in the background.";
    if (!tip.isEmpty()) tooltip(tip, mx, my);
  }
}
