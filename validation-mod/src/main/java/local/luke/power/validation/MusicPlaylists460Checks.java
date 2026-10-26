package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.Validation.*;

import com.google.gson.JsonPrimitive;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.class_267;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/** Real Options controls in a muted disposable client. No fixture data survives stop. */
final class MusicPlaylists460Checks {
  private static final Path REPORT = Path.of("power-beta-data/reports/music-playlists-460");
  private static final String FIRST = "music:calm1.ogg", SECOND = "music:calm2.ogg";
  private static final String MISSING = "music:playlist-fixture-missing.ogg";
  private static State staged;
  private MusicPlaylists460Checks() {}

  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("stop")) { stop(); return; }
    check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "mute the disposable client");
    check(mc.world != null && !mc.world.isRemote && mc.player != null, "open a disposable singleplayer world");
    stop();
    if (action.startsWith("view ")) {
      staged = new State(mc);
      try { stage(mc, action.substring(5), 640, 420); }
      catch (Exception | Error failure) { stop(); throw failure; }
      log("MUSIC460 staged=" + action.substring(5) + "; use music-playlists-460 stop to restore");
      return;
    }
    check(action.equals("capture") || action.equals("run") || action.equals("menu-pools"), "use music-playlists-460 capture|run|menu-pools|view settings|view library|view playlists|view save-current|view editor|view order|view empty|view queue|view menu|stop");
    failures = 0;
    try (var state = new State(mc)) {
      if (action.equals("capture")) capture(mc);
      else if (action.equals("menu-pools")) menuPools();
      else {
        sessions(mc);
        editor(mc);
        saveCurrent(mc);
        menuAssignment(mc);
        selections(mc);
        menuPools();
      }
    }
    log("MUSIC460 " + action + " FAILURES " + failures + " directory=" + REPORT);
    check(failures == 0, "playlist failures=" + failures);
  }

  private static void stop() throws Exception {
    if (staged != null) { State state = staged; staged = null; state.close(); }
  }

  private static AudioSettings seed() {
    var settings = new AudioSettings();
    settings.musicMode = AudioSettings.MusicMode.ALL_MINECRAFT;
    settings.playlists.add(new MusicPlaylist("quiet", "Quiet afternoons", List.of(SECOND, FIRST, MISSING), Map.of("Alpha", 40)));
    settings.playlists.add(new MusicPlaylist("building", "Building", List.of("music:creative4.ogg", "music:creative1.ogg"), Map.of("Beta / Creative", 65)));
    settings.playlists.add(new MusicPlaylist("empty", "Empty", List.of(), Map.of()));
    settings.favourites.add(FIRST); settings.sounds.put(FIRST, 35);
    MusicPlaylist.load(settings, "quiet"); MusicPlaylist.loadMenu(settings, "building");
    return settings;
  }

  private static PowerOptionsScreen open(Minecraft mc) throws Exception {
    var options = MenuUpdateChecks.open(mc, "Audio");
    ((Set<?>)field(options, "collapsed")).clear(); call(options, "layout");
    return options;
  }

  private static MusicLibraryScreen library(PowerOptionsScreen options) throws Exception {
    field(options, "libraryOpen", true); call(options, "layout");
    return (MusicLibraryScreen)field(options, "library");
  }

  private static PowerOptionsScreen stage(Minecraft mc, String view, int width, int height) throws Exception {
    MenuPreferences.update(v -> v.autoApply = false);
    AudioConfig.preview(seed());
    var options = open(mc); options.init(mc, width, height);
    var library = library(options);
    switch (view) {
      case "settings" -> {
        Dropdown450Screens.resetOptions(options, "Audio");
        Object playlistRow = null;
        for (Object row : (List<?>)field(options, "rows")) {
          var setting = (Setting)call(row, "setting");
          if (setting != null && setting.id.equals("audio.playlist")) { playlistRow = row; break; }
        }
        check(playlistRow != null, "world playlist selector missing from unfiltered Audio settings");
        field(options, "scroll", Math.max(0d, (int)call(playlistRow, "y") - 24d));
        call(options, "layout");
        check(((TextInput)field(options, "search")).text().isEmpty(), "settings capture unexpectedly filtered Audio rows");
      }
      case "library" -> library.showTrack(FIRST);
      case "playlists" -> library.openPresets();
      case "save-current" -> {
        library.openPresets(); var panel = field(library, "presets");
        click(options, (int)call(panel, "right") - 100, (int)call(panel, "top") + 8);
        check((boolean)call(field(options, "choices"), "active"), "Save current did not open overwrite/save-as dropdown");
      }
      case "editor", "order" -> {
        library.openPresets(); var panel = field(library, "presets");
        click(options, (int)call(panel, "right") - 145, presetRowY(panel, 1));
        check((boolean)call(panel, "editing"), "playlist Edit missed");
        ((Set<?>)field(library, "collapsed")).clear(); call(library, "rebuildRows");
        if (view.equals("order")) {
          click(options, (int)call(panel, "orderX") + 8, (int)call(panel, "top") + 8);
          check(mc.currentScreen instanceof MusicPlaylistOrderScreen, "Order did not open native screen");
          mc.currentScreen.init(mc, width, height);
        }
      }
      case "empty" -> {
        library.openPresets(); var panel = field(library, "presets");
        click(options, (int)call(panel, "left") + 10, (int)call(panel, "top") + 8);
        check(((MusicPresetDraft)call(panel, "draft")).tracks().isEmpty(), "new playlist is not empty");
      }
      case "queue" -> {
        MusicRequests.edit(q -> { q.tracks.clear(); q.add(FIRST); q.add(SECOND); q.add(FIRST); });
        library.showQueue();
      }
      case "menu" -> {
        library.showTracks(); field(options, "libraryOpen", false);
        Dropdown450Screens.openSetting(options, find(options.session(), "audio.menuPlaylist"));
      }
      default -> throw new IllegalArgumentException("Unknown playlist view " + view);
    }
    mc.currentScreen.render(-1, -1, 0); return options;
  }

  private static void capture(Minecraft mc) throws Exception {
    Files.createDirectories(REPORT);
    var captures = new ArrayList<String>();
    int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      for (int[] size : new int[][]{{320, 240}, {640, 420}})
        for (String view : List.of("settings", "library", "playlists", "save-current", "editor", "order", "empty", "queue", "menu")) {
          var screen = stage(mc, view, size[0], size[1]);
          Screen captured = mc.currentScreen;
          if (view.equals("menu") || view.equals("save-current")) check((boolean)call(field(screen, "choices"), "active"), view + " capture lost its open dropdown");
          String file = view + "-" + size[0] + ".png";
          SettingsSnapshot.render(mc, captured, size[0], size[1], REPORT.resolve(file));
          captures.add(file); screen.session().discard(); screen.removed();
        }
    } finally {
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glPopAttrib(); GL11.glMatrixMode(mode);
    }
    Files.writeString(REPORT.resolve("captures.json"), Catalog.JSON.toJson(captures));
    log("MUSIC460 native captures=" + captures.size());
  }

  private static void sessions(Minecraft mc) throws Exception {
    for (boolean automatic : new boolean[]{false, true})
      test("MUSIC460 real Options Apply and Discard auto=" + automatic, () -> {
        MenuPreferences.update(v -> v.autoApply = automatic); AudioConfig.save(seed());
        var options = open(mc);
        MusicRequests.edit(q -> { q.tracks.clear(); q.add(FIRST); q.add(FIRST); q.add(MISSING); });
        var queued = MusicRequests.tracks();
        var enabled = find(options.session(), "audio.trackEnabled." + SECOND);
        enabled.value = new JsonPrimitive(false); options.changed(enabled);
        check(AudioConfig.current().playlistTracks.equals(List.of(FIRST, MISSING)), "working edit lost order or missing reference");
        check(AudioConfig.current().playlists.get(0).tracks().equals(seed().playlists.get(0).tracks()), "working edit mutated saved playlist");
        check(AudioConfig.current().menuPlaylistTracks.equals(seed().menuPlaylistTracks), "world edit changed menu selection");
        var persisted = PowerConfig.readAudio(AudioSettings.class);
        check(persisted.playlistTracks.equals(automatic ? List.of(FIRST, MISSING) : seed().playlistTracks), "auto/manual persistence boundary failed");
        if (!automatic) click(options, actionX(options, 1) + 8, options.height - 20);
        check(PowerConfig.readAudio(AudioSettings.class).playlistTracks.equals(List.of(FIRST, MISSING)), "Apply did not persist working selection");
        check(MusicRequests.tracks().equals(queued), "Options Apply replaced a newer queue");
        check(PowerConfig.musicQueue().getAsJsonArray("tracks").size() == queued.size(), "queue persistence changed during Apply");
        if (!automatic) {
          enabled = find(options.session(), "audio.trackEnabled." + FIRST);
          enabled.value = new JsonPrimitive(false); options.changed(enabled);
          click(options, actionX(options, 0) + 8, options.height - 20);
          check((boolean)field(options, "confirmClose"), "dirty Options exit skipped confirmation");
          click(options, options.width / 2 + 12, options.height / 2 + 20);
          check(AudioConfig.current().playlistTracks.equals(List.of(FIRST, MISSING)), "Discard lost committed selection");
          check(MusicRequests.tracks().equals(queued), "Discard rolled back the queue");
        } else check(options.session().changes() == 0, "auto-apply left an unsaved draft");
      });
  }

  private static int actionX(PowerOptionsScreen screen, int action) throws Exception {
    return (int)Dropdown450Screens.invoke(screen, "actionX", new Class<?>[]{int.class}, action);
  }

  private static void editor(Minecraft mc) throws Exception {
    test("MUSIC460 editor membership, order and group gain remain isolated until Load", () -> {
      MenuPreferences.update(v -> v.autoApply = false); AudioConfig.preview(seed());
      var options = open(mc); var library = library(options); library.openPresets();
      var panel = field(library, "presets");
      click(options, (int)call(panel, "right") - 145, presetRowY(panel, 1));
      var draft = (MusicPresetDraft)call(panel, "draft");
      var original = draft.tracks();
      click(options, (int)call(panel, "orderX") + 8, (int)call(panel, "top") + 8);
      var order = mc.currentScreen;
      ((ScreenInput)(Object)order).power$click((int)call(order, "right") - 14, 62, 0);
      check(draft.tracks().equals(original), "order editor changed its parent draft before Done");
      ((ScreenInput)(Object)order).power$click(order.width / 2 + 12, order.height - 20, 0);
      check(draft.tracks().equals(original), "order Cancel changed parent draft");
      click(options, (int)call(panel, "orderX") + 8, (int)call(panel, "top") + 8);
      order = mc.currentScreen;
      ((ScreenInput)(Object)order).power$click((int)call(order, "right") - 14, 62, 0);
      ((ScreenInput)(Object)order).power$click(order.width / 2 - 94, order.height - 20, 0);
      check(mc.currentScreen == options, "order Done did not return to the playlist editor");
      check(draft.tracks().equals(List.of(FIRST, SECOND, MISSING)), "order Done lost native row edit");
      check(AudioConfig.current().playlistTracks.equals(original), "order Done edited working state");
      draft.include(SECOND, false); draft.move(MISSING, -1); draft.volume("Alpha", 25);
      check(draft.tracks().equals(List.of(MISSING, FIRST)), "editor order or missing reference lost");
      check(AudioConfig.current().playlistTracks.equals(seed().playlistTracks), "editor changed working membership");
      check(AudioConfig.current().groupVolumes.equals(Map.of("Alpha", 40)), "editor gain leaked into playback");
      click(options, (int)call(panel, "saveX") + 8, (int)call(panel, "editButtonY") + 8);
      check(AudioConfig.current().playlists.get(0).tracks().equals(List.of(MISSING, FIRST)), "Save lost playlist order");
      check(AudioConfig.current().playlistTracks.equals(seed().playlistTracks), "Save loaded the playlist implicitly");
      check(AudioConfig.current().sounds.get(FIRST) == 35, "editor changed per-track gain");
      library.openPresets(); click(options, (int)call(panel, "right") - 25, presetRowY(panel, 1));
      check(AudioConfig.current().playlistTracks.equals(List.of(MISSING, FIRST)), "Load lost membership or order");
      check(AudioConfig.current().groupVolumes.equals(Map.of("Alpha", 25)), "Load did not copy group gain");
      check(AudioConfig.current().menuPlaylistTracks.equals(seed().menuPlaylistTracks), "Load changed menu membership");
      options.session().discard();
    });
    test("MUSIC460 new playlist starts empty and drafts retain missing references", () -> {
      var empty = new MusicPresetDraft(null, List.of(FIRST, SECOND));
      check(empty.tracks().isEmpty(), "new editor copied library membership");
      var restored = new MusicPresetDraft(seed().playlists.get(0), List.of(FIRST, SECOND));
      check(restored.snapshot("Retained").tracks().equals(seed().playlistTracks), "opening editor discarded missing track");
      restored.includeAll(); check(restored.tracks().contains(MISSING), "Include all discarded missing track");
    });
  }

  private static void saveCurrent(Minecraft mc) throws Exception {
    test("MUSIC460 Save current starts prefilled and retains an isolated draft", () -> {
      MenuPreferences.update(v -> v.autoApply = false); AudioConfig.preview(seed());
      var options = open(mc); var library = library(options); library.openPresets();
      var panel = field(library, "presets");
      click(options, (int)call(panel, "right") - 100, (int)call(panel, "top") + 8);
      var popup = field(options, "choices");
      check((boolean)call(popup, "active"), "Save current skipped overwrite/save-as choice");
      ((ScreenInput)(Object)options).power$key('\0', Keyboard.KEY_HOME);
      ((ScreenInput)(Object)options).power$key('\0', Keyboard.KEY_RETURN);
      var draft = (MusicPresetDraft)call(panel, "draft");
      check(draft.tracks().equals(seed().playlistTracks), "Save current did not copy working order/missing entries");
      check(draft.volume("Alpha") == 40, "Save current did not copy group gain");
      draft.include(FIRST, false);
      check(AudioConfig.current().playlistTracks.equals(seed().playlistTracks), "Save current editor changed working state");
      check(AudioConfig.current().playlists.get(0).tracks().equals(seed().playlistTracks), "Save current editor changed saved state");
      options.session().discard();
    });
  }

  private static void menuAssignment(Minecraft mc) throws Exception {
    test("MUSIC460 native menu assignment copies ordered membership independently", () -> {
      MenuPreferences.update(v -> v.autoApply = false); AudioConfig.preview(seed());
      var options = open(mc);
      Dropdown450Screens.openSetting(options, find(options.session(), "audio.menuPlaylist"));
      ((ScreenInput)(Object)options).power$key('\0', Keyboard.KEY_HOME);
      ((ScreenInput)(Object)options).power$key('\0', Keyboard.KEY_END);
      ((ScreenInput)(Object)options).power$key('\0', Keyboard.KEY_RETURN);
      check(AudioConfig.current().menuPlaylist.equals("empty"), "menu selector did not choose last saved playlist");
      check(AudioConfig.current().menuPlaylistTracks.isEmpty(), "empty menu playlist expanded implicitly");
      check(AudioConfig.current().playlistTracks.equals(seed().playlistTracks), "menu selector changed world membership");
      options.session().discard();
      check(AudioConfig.current().menuPlaylist.equals("building") && AudioConfig.current().menuPlaylistTracks.equals(seed().menuPlaylistTracks), "menu assignment Discard lost previous selection");
    });
  }

  private static void selections(Minecraft mc) throws Exception {
    test("MUSIC460 stored menu sentinels and world Default/empty selections remain distinct", () -> {
      var settings = seed(); var world = List.copyOf(settings.playlistTracks);
      AudioConfig.preview(settings);
      check(List.copyOf(AudioController.activeMusic(mc)).equals(List.of(SECOND, FIRST)), "effective world order changed or missing entry became playable");
      MusicPlaylist.loadMenu(settings, "empty");
      check(settings.menuPlaylistTracks.isEmpty() && settings.menuPlaylist.equals("empty"), "empty menu playlist fell back to Default");
      check(settings.playlistTracks.equals(world), "menu assignment edited world selection");
      MusicPlaylist.loadMenu(settings, MusicPlaylist.DEFAULT);
      check(settings.menuPlaylist.equals(MusicPlaylist.DEFAULT), "menu Default assignment lost");
      MusicPlaylist.loadMenu(settings, MusicPlaylist.SAME_AS_WORLD);
      check(settings.menuPlaylist.equals(MusicPlaylist.SAME_AS_WORLD), "menu same-as-world assignment lost");
      settings.disabledTracks.add(FIRST); MusicPlaylist.load(settings, "");
      check(settings.playlist.isEmpty() && settings.playlistTracks.isEmpty(), "world Default kept working playlist");
      check(settings.disabledTracks.contains(FIRST), "world Default changed library exclusion");
      MusicPlaylist.load(settings, "empty");
      check(settings.playlist.equals("empty") && settings.playlistTracks.isEmpty(), "empty world playlist fell back to Default");
      settings.validate();
      AudioConfig.preview(settings);
      check(AudioController.activeMusic(mc).isEmpty(), "empty world playlist admitted default tracks");
    });
  }

  private static void menuPools() throws Exception {
    Class<?> contextType = Class.forName("local.luke.power.audio.AudioController$Context");
    var constructor = contextType.getDeclaredConstructor(boolean.class, int.class, String.class);
    constructor.setAccessible(true);
    Object menuContext = constructor.newInstance(false, 0, null);
    var pool = AudioController.class.getDeclaredMethod("backgroundPool", List.class, contextType, boolean.class);
    var origin = AudioController.class.getDeclaredMethod("usesMenuVolumes", String.class, contextType);
    pool.setAccessible(true); origin.setAccessible(true);
    Class<?> previewType = Class.forName("local.luke.power.audio.MusicPreview");
    var activePreview = previewType.getDeclaredMethod("active"); activePreview.setAccessible(true);
    check(!(boolean)activePreview.invoke(null), "stop the active music preview before menu pool/gain checks");
    String creative = "music:creative4.ogg", other = "music:creative1.ogg";
    List<String> worldIds = List.of(SECOND, FIRST);
    List<String> savedIds = List.of(FIRST, creative, SECOND, other);
    List<String> defaultIds = List.of(other, SECOND, creative);
    List<class_267> vanilla = new ArrayList<>();
    for (String id : List.of(other, FIRST, other, SECOND, creative)) {
      var track = AudioController.resolveTrack(id); check(track != null, "native fixture track missing " + id); vanilla.add(track);
    }
    for (var mode : AudioSettings.MenuMusic.values())
      for (String assignment : List.of(MusicPlaylist.SAME_AS_WORLD, MusicPlaylist.DEFAULT, "building", "empty"))
        test("MUSIC460 native menu pool and current-source gain " + mode + " " + assignment, () -> {
          var settings = seed(); settings.master = 80;
          settings.disabledTracks.add(FIRST);
          settings.sounds.put(creative, 50);
          settings.groupVolumes = new TreeMap<>(Map.of("Alpha", 40, "Beta / Creative", 80));
          settings.playlists.set(1, new MusicPlaylist("building", "Building", savedIds,
              Map.of("Alpha", 25, "Beta / Creative", 65)));
          settings.menuMusic = mode; MusicPlaylist.loadMenu(settings, assignment);
          AudioConfig.preview(settings);
          List<String> selectedMenu = assignment.equals(MusicPlaylist.DEFAULT) ? defaultIds
              : assignment.equals("building") ? savedIds : List.of();
          List<String> expected = new ArrayList<>(worldIds);
          boolean separate = mode != AudioSettings.MenuMusic.WORLD && !assignment.equals(MusicPlaylist.SAME_AS_WORLD);
          if (separate) {
            if (mode == AudioSettings.MenuMusic.CUSTOM) expected.clear();
            for (String id : selectedMenu) if (!expected.contains(id)) expected.add(id);
          }
          @SuppressWarnings("unchecked")
          List<class_267> tracks = (List<class_267>)pool.invoke(null, vanilla, menuContext, false);
          List<String> actual = tracks.stream().map(AudioController::trackId).toList();
          check(actual.equals(expected), "menu pool order/membership " + actual + " expected " + expected);
          check(AudioConfig.current().playlistTracks.equals(seed().playlistTracks), "menu pool changed stored world/missing references");
          check(AudioConfig.current().menuPlaylistTracks.equals(settings.menuPlaylistTracks), "menu pool changed stored menu membership/order");
          for (String id : actual) {
            boolean expectedMenuGain = separate && assignment.equals("building")
                && (mode == AudioSettings.MenuMusic.CUSTOM || !worldIds.contains(id));
            boolean menuGain = (boolean)origin.invoke(null, id, menuContext);
            check(menuGain == expectedMenuGain, "wrong world/menu gain owner for " + id);
            float group = expectedMenuGain ? id.startsWith("music:calm") ? .25f : .65f
                : id.startsWith("music:calm") ? .4f : .8f;
            float track = id.equals(FIRST) ? .35f : id.equals(creative) ? .5f : 1;
            float expectedGain = .8f * track * group;
            Object originalTrack = staticField(AudioController.class, "currentMusic");
            Object originalOrigin = staticField(AudioController.class, "currentUsesMenuVolumes");
            Object originalPaused = staticField(AudioController.class, "paused");
            try {
              // Exercise the latched current-source gain path without unloading a world or starting audio.
              staticField(AudioController.class, "currentMusic", id);
              staticField(AudioController.class, "currentUsesMenuVolumes", menuGain);
              staticField(AudioController.class, "paused", false);
              check(Math.abs(AudioController.backgroundVolume(1f) - expectedGain) < .00001f,
                  "native current-source gain differs for " + id + ": " + AudioController.backgroundVolume(1f));
            } finally {
              staticField(AudioController.class, "currentMusic", originalTrack);
              staticField(AudioController.class, "currentUsesMenuVolumes", originalOrigin);
              staticField(AudioController.class, "paused", originalPaused);
            }
          }
        });
  }

  private static final class State implements AutoCloseable {
    private final Minecraft mc;
    private final Screen previous;
    private final AudioSettings audio;
    private final Object queue, menu;
    private final boolean repeat;
    private final Map<Path, byte[]> files = new LinkedHashMap<>();
    private final Map<Path, Set<Path>> backups = new LinkedHashMap<>();
    private final Set<Path> newBackupDirectories = new HashSet<>();

    State(Minecraft mc) throws Exception {
      this.mc = mc; previous = mc.currentScreen; repeat = Keyboard.areRepeatEventsEnabled();
      PowerConfig.flushPending(); audio = AudioConfig.copy();
      queue = staticField(MusicRequests.class, "queue"); menu = staticField(MenuPreferences.class, "current");
      for (Path path : List.of(PowerConfig.path(), PowerConfig.musicPath())) {
        files.put(path, Files.exists(path) ? Files.readAllBytes(path) : null);
        Path directory = path.getParent().resolve("backups");
        if (!Files.exists(directory)) newBackupDirectories.add(directory);
        try (var children = Files.exists(directory) ? Files.list(directory) : java.util.stream.Stream.<Path>empty()) {
          backups.put(directory, new HashSet<>(children.toList()));
        }
      }
    }

    @Override public void close() throws Exception {
      Exception discardFailure = null;
      if (mc.currentScreen instanceof PowerOptionsScreen options) try { options.session().discard(); options.removed(); }
      catch (Exception failure) { discardFailure = failure; }
      AudioConfig.preview(audio); staticField(MusicRequests.class, "queue", queue); staticField(MenuPreferences.class, "current", menu);
      PowerConfig.flushPending();
      for (var entry : files.entrySet()) {
        if (entry.getValue() == null) Files.deleteIfExists(entry.getKey());
        else Files.write(entry.getKey(), entry.getValue());
      }
      for (var entry : backups.entrySet()) if (Files.exists(entry.getKey())) {
        try (var children = Files.list(entry.getKey())) {
          for (Path path : children.toList()) if (!entry.getValue().contains(path)) Files.delete(path);
        }
      }
      for (Path directory : newBackupDirectories) Files.deleteIfExists(directory);
      mc.setScreen(previous); Keyboard.enableRepeatEvents(repeat);
      for (var entry : files.entrySet()) check(entry.getValue() == null ? !Files.exists(entry.getKey()) : Arrays.equals(entry.getValue(), Files.readAllBytes(entry.getKey())), "fixture did not restore " + entry.getKey().getFileName());
      check(Catalog.JSON.toJson(AudioConfig.current()).equals(Catalog.JSON.toJson(audio)), "fixture did not restore runtime audio");
      if (discardFailure != null) throw discardFailure;
    }
  }

  private static Object staticField(Class<?> type, String name) throws Exception {
    Field field = type.getDeclaredField(name); field.setAccessible(true); return field.get(null);
  }
  private static void staticField(Class<?> type, String name, Object value) throws Exception {
    Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(null, value);
  }
}
