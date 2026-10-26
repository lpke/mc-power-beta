package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import com.google.gson.JsonPrimitive;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;

/** Native playback, transaction and control geometry checks in a muted disposable world. */
final class Release463Checks {
  private static final String FIRST = "music:calm1.ogg", SECOND = "music:calm2.ogg", THIRD = "music:creative1.ogg";
  private static final String CUSTOM = "ui462-long";
  private static final int[][] SIZES = {{854, 480}, {320, 240}};
  private static final Path REPORT = Path.of("power-beta-data/reports/release-463/ui");

  static void run(Minecraft mc, String action) throws Exception {
    check(action.equals("capture") || action.equals("check"), "use release-463 capture|check");
    SilentAudioChecks.ready(mc);
    check(mc.world != null && !mc.world.isRemote && mc.options.musicVolume == 0 && mc.options.soundVolume == 0,
        "use a muted disposable singleplayer world");
    check(AudioController.currentTrackId().isEmpty(), "start with idle music");
    failures = 0;
    var selector = (TrackSelector)stat("WORLD");
    var cursorMap = (Map<Object,Object>)get(selector, "cursors");
    var cursors = new LinkedHashMap<>(cursorMap);
    Object previous = get(selector, "previous"), index = stat("historyIndex");
    var history = new ArrayList<>((List<?>)stat("history"));
    var origins = new ArrayList<>((List<?>)stat("historyPlayback"));
    try (var state = new Release462Checks.State(mc)) {
      if (action.equals("capture")) capture(mc, state);
      else {
        test("RELEASE463 full-size sliders and editor action hit bounds at both sizes", () -> controls(mc, state));
        test("RELEASE463 saved playlist play selects the world playlist and advances from that song", () -> playlist(state));
        test("RELEASE463 Library and queue member plays align order while outside songs do not", () -> libraryAndQueue(state));
        test("RELEASE463 dirty drafts, same-song pause/resume and manual Cancel retain ownership", () -> drafts(state));
        test("RELEASE463 now-playing links route members, outsiders and auditions with gold names", () -> navigation(mc, state));
        test("RELEASE463 separate menu assignment retains both playlist cursors", () -> menu(mc, state));
      }
    } finally {
      AudioController.quiet();
      ((List<Object>)stat("history")).clear(); ((List<Object>)stat("history")).addAll(history);
      ((List<Object>)stat("historyPlayback")).clear(); ((List<Object>)stat("historyPlayback")).addAll(origins);
      stat("historyIndex", index);
      cursorMap.clear(); cursorMap.putAll(cursors); set(selector, "previous", previous);
    }
    log("RELEASE463 " + action + " failures=" + failures);
    check(failures == 0, "release-463 failures=" + failures);
  }

  private static PowerOptionsScreen open(Release462Checks.State state, int[] size, String active) throws Exception {
    AudioController.quiet();
    ((List<?>)stat("history")).clear(); ((List<?>)stat("historyPlayback")).clear(); stat("historyIndex", -1);
    var selector = (TrackSelector)stat("WORLD"); ((Map<?,?>)get(selector, "cursors")).clear(); set(selector, "previous", "");
    var options = state.open(size, active, false); state.playback = true;
    setting(options, "audio.shuffle").value = new JsonPrimitive(false);
    call(options, "changed", new Class<?>[]{Setting.class}, setting(options, "audio.shuffle"));
    return options;
  }
  private static void capture(Minecraft mc, Release462Checks.State state) throws Exception {
    var names = new ArrayList<String>();
    for (int[] size : SIZES) {
      var options = open(state, size, MusicPlaylist.BETA);
      shot(mc, options, "audio-volume", size, names);
      if (size[0] == 320) {
        Object row = row(options, "native.sound");
        set(options, "scroll", Math.max(0d, (int)call(row, "y") - 32d)); call(options, "layout");
        shot(mc, options, "audio-volume-scrolled", size, names);
        set(options, "scroll", 0d);
      }
      ((TextInput)get(options, "search")).setText("HUD editor"); call(options, "layout");
      shot(mc, options, "hud-edit", size, names);
      options = open(state, size, MusicPlaylist.BETA); var library = library(options);
      edit(library, CUSTOM); play(options, library, SECOND);
      shot(mc, options, "playlist-play", size, names);
      options = open(state, size, MusicPlaylist.BETA); library = library(options);
      AudioController.playNow(THIRD); library.showActiveTrack(THIRD);
      shot(mc, options, "outside-library", size, names);
      options = open(state, size, MusicPlaylist.BETA); library = library(options);
      AudioController.previewSound(FIRST); library.showActiveTrack(FIRST);
      shot(mc, options, "preview-library", size, names);
    }
    Files.writeString(REPORT.resolve("captures.json"), Catalog.JSON.toJson(names));
    log("RELEASE463 captures=" + names.size());
  }
  private static void controls(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) {
      var options = open(state, size, MusicPlaylist.BETA);
      for (String id : List.of("audio.master", "native.music", "native.sound", "audio.category.blocks")) {
        Object row = row(options, id);
        int base = (int)call(options, "controlLeft", new Class<?>[]{row.getClass()}, row);
        check(base == (int)call(options, "mainControlLeft", new Class<?>[]{row.getClass()}, row), "slider shortened: " + id);
        if (!id.startsWith("native.")) continue;
        int y = n(options, "top") + (int)call(row, "y") - ((Number)get(options, "scroll")).intValue();
        int x = (int)call(options, "audioActionX", new Class<?>[]{row.getClass()}, row);
        int top = (int)call(options, "audioActionY", new Class<?>[]{row.getClass(), int.class}, row, y);
        int height = (int)call(options, "audioActionHeight", new Class<?>[]{row.getClass()}, row);
        check(x >= n(options, "left") && x + 44 <= n(options, "right"), "action leaves content bounds");
        if ((int)call(row, "height") == 40) check(top + height < y + 16, "compact action overlaps slider");
        else check(x + 48 == base, "action is not before the full slider");
      }
      for (String id : List.of("native.music", "native.sound")) {
        options = open(state, size, MusicPlaylist.BETA); Object row = row(options, id);
        set(options, "scroll", Math.max(0d, (int)call(row, "y") - 32d)); call(options, "layout");
        int y = n(options, "top") + (int)call(row, "y") - ((Number)get(options, "scroll")).intValue();
        click(options, (int)call(options, "audioActionX", new Class<?>[]{row.getClass()}, row) + 3,
            (int)call(options, "audioActionY", new Class<?>[]{row.getClass(), int.class}, row, y) + 3);
        check(id.equals("native.music") ? (boolean)call(options, "libraryVisible") : (boolean)get(options, "allSounds"),
            "editor action missed its drawn hit target: " + id + " at " + size[0]);
      }
      options = open(state, size, MusicPlaylist.BETA);
      ((TextInput)get(options, "search")).setText("HUD editor"); call(options, "layout");
      var accepts = Class.forName("local.luke.power.ui.SettingsDropdown").getDeclaredMethod("accepts", Setting.class); accepts.setAccessible(true);
      check(!(boolean)accepts.invoke(null, setting(options, HudEditorSettings.ENTRY)), "HUD Edit still classified as a dropdown");
      Object row = row(options, HudEditorSettings.ENTRY);
      click(options, (int)call(options, "mainControlLeft", new Class<?>[]{row.getClass()}, row) + 5,
          n(options, "top") + (int)call(row, "y") - ((Number)get(options, "scroll")).intValue() + ((int)call(row, "height") == 40 ? 20 : 8));
      check(mc.currentScreen instanceof HudEditorScreen, "plain HUD Edit lost its editor route");
    }
  }
  private static void playlist(Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) {
      var options = open(state, size, MusicPlaylist.BETA); var library = library(options);
      edit(library, CUSTOM); play(options, library, SECOND);
      check(AudioConfig.current().playlist.equals(CUSTOM), "playlist-row play did not select its playlist");
      check(AudioController.currentTrackId().equals(SECOND), "playlist-row play chose another song");
      AudioController.next(); check(AudioController.currentTrackId().equals(THIRD), "ordered playlist did not continue after clicked member");
      AudioController.next(); check(AudioController.currentTrackId().equals(FIRST), "ordered playlist did not wrap");
      options = open(state, size, MusicPlaylist.BETA); library = library(options);
      edit(library, MusicPlaylist.FAVORITES); play(options, library, FIRST);
      check(AudioConfig.current().playlist.equals(MusicPlaylist.FAVORITES), "Favorites row did not select its playlist");
    }
  }
  private static void libraryAndQueue(Release462Checks.State state) throws Exception {
    var options = open(state, SIZES[0], CUSTOM); var library = library(options);
    library.showTrack(SECOND); play(options, library, SECOND);
    check(AudioConfig.current().playlist.equals(CUSTOM), "Library member changed the world playlist");
    AudioController.next(); check(AudioController.currentTrackId().equals(THIRD), "Library member did not align continuation");
    AudioController.playNow("music:creative2.ogg");
    check(AudioConfig.current().playlist.equals(CUSTOM), "outside song changed playlist");
    AudioController.next(); check(AudioController.currentTrackId().equals(FIRST), "outside song moved playlist cursor");
    open(state, SIZES[0], CUSTOM);
    MusicRequests.edit(q -> { q.add(SECOND); q.add(FIRST); });
    AudioController.playQueue(); check(AudioController.currentTrackId().equals(SECOND), "queued member did not play");
    AudioController.next(); check(AudioController.currentTrackId().equals(FIRST) && MusicRequests.tracks().isEmpty(), "queue priority or consumption changed");
    AudioController.next(); check(AudioController.currentTrackId().equals(SECOND), "queue member did not align cursor");
    open(state, SIZES[0], CUSTOM); MusicRequests.edit(q -> q.add(SECOND));
    check(AudioController.trackId(AudioController.choose()).equals(SECOND), "native queued acceptance did not choose member");
    AudioController.next(); check(AudioController.currentTrackId().equals(THIRD), "native queued acceptance did not align cursor");
  }
  private static void drafts(Release462Checks.State state) throws Exception {
    var options = open(state, SIZES[0], MusicPlaylist.BETA); var library = library(options); edit(library, CUSTOM);
    var draft = draft(library); var saved = MusicPlaylist.find(AudioConfig.current(), CUSTOM).tracks();
    draft.order(List.of(THIRD, SECOND, FIRST)); draft.include("music:creative2.ogg", true);
    call(library, "syncPanel"); AudioController.playNow(SECOND);
    play(options, library, SECOND);
    check(AudioConfig.current().playlist.equals(CUSTOM) && (boolean)stat("paused"), "same-song cross-playlist click lost pause behavior");
    play(options, library, SECOND); check(!(boolean)stat("paused"), "same-song click did not resume");
    AudioController.next(); check(AudioController.currentTrackId().equals(THIRD), "unsaved reorder leaked into playback order");
    check(draft(library) == draft && library.playlistDirty() && MusicPlaylist.find(AudioConfig.current(), CUSTOM).tracks().equals(saved),
        "playing saved the draft or replaced its owner");
    options.session().discard();
    check(AudioConfig.current().playlist.equals(MusicPlaylist.BETA), "manual Cancel did not restore playlist selection");
    options = open(state, SIZES[0], MusicPlaylist.BETA); library = library(options); library.openPresets();
    call(get(library, "presets"), "newPlaylist"); call(library, "syncPanel");
    draft(library).include(SECOND, true); call(library, "syncPanel"); play(options, library, SECOND);
    check(AudioConfig.current().playlist.equals(MusicPlaylist.BETA), "unsaved new playlist selected an invalid ID");
    options = open(state, SIZES[0], MusicPlaylist.BETA); library = library(options); edit(library, CUSTOM);
    var unchanged = MusicPlaylist.find(AudioConfig.current(), CUSTOM).tracks();
    draft(library).include("music:creative2.ogg", true); call(library, "syncPanel");
    MenuPreferences.current().autoApply = true;
    try {
      play(options, library, SECOND); local.luke.power.storage.PowerConfig.flushPending();
      check(AudioConfig.current().playlist.equals(CUSTOM) && MusicPlaylist.find(AudioConfig.current(), CUSTOM).tracks().equals(unchanged)
          && library.playlistDirty(), "Auto apply saved an unfinished playlist or lost playback selection");
      AudioController.next(); check(AudioController.currentTrackId().equals(THIRD), "Auto apply changed saved playback order");
    } finally { MenuPreferences.current().autoApply = false; }
  }
  private static void navigation(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) for (int mode = 0; mode < 3; mode++) {
      var options = open(state, size, MusicPlaylist.BETA); var library = (MusicLibraryScreen)get(options, "library");
      String id = mode == 1 ? THIRD : FIRST;
      if (mode == 2) AudioController.previewSound(id); else AudioController.playNow(id);
      options.render(-1, -1, 0); Object status = get(options, "musicStatus");
      click(options, (int)get(status, "labelX") + 3, (int)get(status, "top") + 5);
      check((boolean)call(library, "activePlaylistTab") == (mode == 0), "Options now-playing jump chose wrong destination: "
          + size[0] + " mode=" + mode + " folder=" + get(library, "folder") + " current=" + AudioController.currentTrackId());
      check(((String)get(library, "error")).isEmpty(), "now-playing jump shows outside-playlist error");
      checkGold(mc, options, library, id, size, "link-" + mode);
      options.session().discard();
      PowerOptionsScreen.openMusic(mc, options, false, id);
      var linked = (PowerOptionsScreen)mc.currentScreen; state.options = linked; linked.init(mc, size[0], size[1]);
      var target = (MusicLibraryScreen)get(linked, "library");
      check((boolean)call(target, "activePlaylistTab") == (mode == 0), "menu now-playing entrypoint chose wrong destination");
      checkGold(mc, linked, target, id, size, "menu-link-" + mode);
    }
  }
  private static void menu(Minecraft mc, Release462Checks.State state) throws Exception {
    open(state, SIZES[0], CUSTOM);
    var settings = AudioConfig.copy(); settings.menuPlaylist = MusicPlaylist.BETA; AudioConfig.preview(settings);
    var world = mc.world; mc.world = null;
    try {
      AudioController.playNow(FIRST);
      AudioController.next(); check(AudioController.currentTrackId().equals(SECOND), "menu member did not align its own cursor");
      check(AudioConfig.current().playlist.equals(CUSTOM) && AudioConfig.current().menuPlaylist.equals(MusicPlaylist.BETA),
          "explicit playback changed separate menu assignment");
      var selector = (TrackSelector)stat("WORLD");
      check(selector.reserve("playlist:" + CUSTOM, List.of(FIRST, SECOND,THIRD), false, false, new Random(1), id -> id).equals(SECOND),
          "menu explicit member did not align world cursor");
    } finally { mc.world = world; }
  }
  private static MusicLibraryScreen library(PowerOptionsScreen options) throws Exception { call(options, "editMusic"); return (MusicLibraryScreen)get(options, "library"); }
  private static void edit(MusicLibraryScreen library, String id) throws Exception {
    library.openPresets(); call(get(library, "presets"), "edit", new Class<?>[]{MusicPlaylist.class}, MusicPlaylist.find(AudioConfig.current(), id)); call(library, "syncPanel");
  }
  private static MusicPresetDraft draft(MusicLibraryScreen library) throws Exception { return (MusicPresetDraft)call(get(library, "presets"), "draft"); }
  private static int trackY(MusicLibraryScreen library, String id) throws Exception {
    for (Object row : (List<?>)get(library, "rows")) if (id.equals(call(row, "id"))) {
      call(library, "scroll", new Class<?>[]{int.class}, Math.max(0, (int)call(row, "y") - 24));
      return n(library, "listTop") + (int)call(row, "y") - n(library, "scroll");
    }
    throw new AssertionError("song missing from view: " + id);
  }
  private static void play(PowerOptionsScreen options, MusicLibraryScreen library, String id) throws Exception {
    var layout = (MusicRowLayout)call(library, "rowLayout");
    click(options, layout.playX() + 5, trackY(library, id) + layout.controlsY() + 5);
  }
  private static void checkGold(Minecraft mc, PowerOptionsScreen options, MusicLibraryScreen library, String id, int[] size, String label) throws Exception {
    int y = trackY(library, id); shot(mc, options, label, size, null);
    var image = ImageIO.read(REPORT.resolve(label + "-" + size[0] + ".png").toFile());
    var row = (MusicRowLayout)call(library, "rowLayout"); int ink = 0;
    for (int py = (y + 7) * 2; py < (y + 15) * 2; py++)
      for (int px = row.nameX() * 2; px < (row.nameX() + row.nameWidth()) * 2; px++)
        if ((image.getRGB(px, py) & 0xffffff) == 0xffdd66) ink++;
    check(ink > 0, "playing name is not gold at navigation destination");
  }
  private static Object row(PowerOptionsScreen options, String id) throws Exception {
    for (Object row : (List<?>)get(options, "rows")) { var value = (Setting)call(row, "setting"); if (value != null && value.id.equals(id)) return row; }
    throw new AssertionError("setting missing from view: " + id);
  }
  private static Setting setting(PowerOptionsScreen options, String id) { return options.session().settings().stream().filter(s -> s.id.equals(id)).findFirst().orElseThrow(); }
  private static void shot(Minecraft mc, Screen screen, String label, int[] size, List<String> names) throws Exception {
    Files.createDirectories(REPORT); String name = label + "-" + size[0] + ".png";
    SettingsSnapshot.render(mc, screen, size[0], size[1], REPORT.resolve(name), -1, -1); if (names != null) names.add(name);
  }
  private static void click(Screen screen, int x, int y) { ((ScreenInput)(Object)screen).power$click(x, y, 0); }
  private static int n(Object owner, String name) throws Exception { return ((Number)call(owner, name)).intValue(); }
  private static Field field(Class<?> type, String name) throws Exception {
    for (; type != null; type = type.getSuperclass()) try { var f = type.getDeclaredField(name); f.setAccessible(true); return f; } catch (NoSuchFieldException ignored) {}
    throw new NoSuchFieldException(name);
  }
  private static Object get(Object owner, String name) throws Exception { return field(owner.getClass(), name).get(owner); }
  private static void set(Object owner, String name, Object value) throws Exception { field(owner.getClass(), name).set(owner, value); }
  private static Object stat(String name) throws Exception { return field(AudioController.class, name).get(null); }
  private static void stat(String name, Object value) throws Exception { field(AudioController.class, name).set(null, value); }
  private static Object call(Object owner, String name) throws Exception { return call(owner, name, new Class<?>[0]); }
  private static Object call(Object owner, String name, Class<?>[] types, Object... args) throws Exception {
    for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) try {
      var m = type.getDeclaredMethod(name, types); m.setAccessible(true); return m.invoke(owner, args);
    } catch (NoSuchMethodException ignored) {}
    throw new NoSuchMethodException(name);
  }
}
