package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;

import com.google.gson.JsonPrimitive;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.Display;

/** Native pointer acceptance for Music row playback and compact preference/source controls. */
final class Release470MusicUiChecks {
  private static final String FIRST = "music:calm1.ogg", SECOND = "music:calm2.ogg", CUSTOM = "ui462-long";
  private static final Path REPORT = Path.of("power-beta-data/reports/release-470/music-ui");
  private Release470MusicUiChecks() {}

  static void run(Minecraft mc, String action) throws Exception {
    check(Set.of("capture", "check").contains(action), "use release-470-music-ui capture|check");
    SilentAudioChecks.ready(mc);
    check(mc.world != null && mc.player != null && !mc.world.isRemote, "open a disposable singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable client");
    check(Display.isActive(), "focus the disposable client for pointer checks");
    failures = 0;
    Files.createDirectories(REPORT);
    try (var state = new Release462Checks.State(mc)) {
      state.playback = true;
      for (int[] size : List.of(new int[]{854, 480}, new int[]{320, 240})) {
        if (action.equals("capture")) { capture(mc, state, size); continue; }
        test("RELEASE470 row double-click and preview preference at " + size[0], () -> rows(mc, state, size));
        test("RELEASE470 missing-source warning and compact footer at " + size[0], () -> warnings(mc, state, size));
      }
    }
    log("RELEASE470 MUSIC UI failures=" + failures);
    check(failures == 0, "release-470-music-ui failures=" + failures);
  }

  private static void capture(Minecraft mc, Release462Checks.State state, int[] size) throws Exception {
    var options = state.open(size, CUSTOM, false); var library = library(options); library.showPlaylist(CUSTOM);
    var draft = (MusicPresetDraft)call(field(library, "presets"), "draft");
    draft.include(SECOND, false); call(library, "syncPanel");
    SettingsSnapshot.render(mc, options, size[0], size[1], REPORT.resolve("playlist-footer-" + size[0] + ".png"), -1, -1);
    library.toggleDoubleClick();
    find(options.session(), "audio.showMusicPreviewButton").value = new JsonPrimitive(true);
    options.changed(find(options.session(), "audio.showMusicPreviewButton"));
    SettingsSnapshot.render(mc, options, size[0], size[1], REPORT.resolve("preview-footer-" + size[0] + ".png"), -1, -1);
    library.cancelPlaylist(); library.showTracks();
    SettingsSnapshot.render(mc, options, size[0], size[1], REPORT.resolve("library-footer-" + size[0] + ".png"), -1, -1);
    Object folders = field(library, "folders");
    String source = "fixture-capture-missing-source-" + size[0];
    var missing = new MusicLibrarySources.Folder(MusicLibrarySources.id(source), source, "Moved music", "Unavailable", List.of(), List.of());
    invoke(folders, "noticeMissing", new Class<?>[]{Collection.class}, List.of(missing));
    SettingsSnapshot.render(mc, options, size[0], size[1], REPORT.resolve("missing-source-" + size[0] + ".png"), -1, -1);
    call(folders, "cancel"); options.session().discard();
  }

  private static MusicLibraryScreen library(PowerOptionsScreen options) throws Exception {
    call(options, "editMusic"); return (MusicLibraryScreen)field(options, "library");
  }

  private static void rows(Minecraft mc, Release462Checks.State state, int[] size) throws Exception {
    var options = state.open(size, MusicPlaylist.BETA, false); var library = library(options);
    library.showPlaylist(CUSTOM);
    Object panel = field(library, "presets"); var draft = (MusicPresetDraft)call(panel, "draft");
    draft.include(SECOND, false); call(library, "syncPanel");
    List<String> saved = MusicPlaylist.find(AudioConfig.current(), CUSTOM).tracks();
    check(!library.doubleClickPreview(), "double-click did not default to Play");
    var layout = (MusicRowLayout)call(library, "rowLayout");
    check(layout.previewX() == -1, "preview icon did not default to hidden");
    doubleClick(options, library, FIRST);
    check(AudioConfig.current().playlist.equals(CUSTOM), "double-click bypassed row Play's saved playlist selection");
    check(AudioController.currentTrackId().equals(FIRST), "double-click did not play selected song");
    check(library.playlistDirty() && call(panel, "draft") == draft, "double-click replaced the isolated draft");
    check(MusicPlaylist.find(AudioConfig.current(), CUSTOM).tracks().equals(saved), "double-click saved draft membership");
    AudioController.quiet();
    Object footer = call(options, "musicFooter");
    click(options, n(footer, "buttonX") + 4, n(footer, "y") + 7);
    check(library.doubleClickPreview() && find(options.session(), "audio.musicDoubleClickPreview").value.getAsBoolean(), "footer preference did not reach Audio setting");
    find(options.session(), "audio.showMusicPreviewButton").value = new JsonPrimitive(true);
    options.changed(find(options.session(), "audio.showMusicPreviewButton"));
    layout = (MusicRowLayout)call(library, "rowLayout");
    check(layout.favouriteX() < layout.previewX() && layout.previewX() < layout.playX(), "preview icon is outside favourite/play controls");
    doubleClick(options, library, FIRST);
    check(AudioController.previewing(FIRST), "Preview double-click did not audition");
    check(MusicPlaylist.find(AudioConfig.current(), CUSTOM).tracks().equals(saved) && library.playlistDirty(), "Preview saved the playlist draft");
    SettingsSnapshot.render(mc, options, size[0], size[1], REPORT.resolve("preview-footer-" + size[0] + ".png"), -1, -1);
    AudioController.quiet(); options.session().discard();
  }

  private static void warnings(Minecraft mc, Release462Checks.State state, int[] size) throws Exception {
    var options = state.open(size, CUSTOM, false); var library = library(options); library.showTracks();
    Object folders = field(library, "folders");
    String source = "fixture-missing-source-" + size[0];
    var missing = new MusicLibrarySources.Folder(MusicLibrarySources.id(source), source, "Moved music", "Unavailable", List.of(), List.of());
    invoke(folders, "noticeMissing", new Class<?>[]{Collection.class}, List.of(missing));
    check(library.modal(), "missing source did not show a warning");
    SettingsSnapshot.render(mc, options, size[0], size[1], REPORT.resolve("missing-source-" + size[0] + ".png"), -1, -1);
    call(folders, "cancel");
    invoke(folders, "noticeMissing", new Class<?>[]{Collection.class}, List.of(missing));
    check(!library.modal(), "same missing source repeated its session warning");
    Object footer = call(options, "musicFooter");
    check(n(footer, "buttonX") + n(footer, "buttonWidth") <= size[0] - 8, "Music preference leaves the screen");
    SettingsSnapshot.render(mc, options, size[0], size[1], REPORT.resolve("library-footer-" + size[0] + ".png"), -1, -1);
    options.session().discard();
  }

  private static void doubleClick(PowerOptionsScreen options, MusicLibraryScreen library, String id) throws Exception {
    invoke(library, "revealTrack", new Class<?>[]{String.class}, id);
    var row = ((List<?>)field(library, "rows")).stream().filter(r -> {
      try { return id.equals(call(r, "id")); } catch (Exception e) { throw new RuntimeException(e); }
    }).findFirst().orElseThrow();
    var layout = (MusicRowLayout)call(library, "rowLayout");
    int x = layout.nameX() + 5, y = n(library, "listTop") + n(row, "y") - n(library, "scroll") + 9;
    for (int i = 0; i < 2; i++) {
      click(options, x, y);
      ((ScreenInput)(Object)options).power$release(x, y, 0);
    }
  }

  private static int n(Object owner, String key) throws Exception { return ((Number)call(owner, key)).intValue(); }
  private static Object invoke(Object owner, String key, Class<?>[] types, Object... values) throws Exception {
    Method method = owner.getClass().getDeclaredMethod(key, types); method.setAccessible(true); return method.invoke(owner, values);
  }
}
