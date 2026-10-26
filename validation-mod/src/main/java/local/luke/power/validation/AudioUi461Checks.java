package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/** Actual Options controls, isolated playlist edits and screenshots in the muted native client. */
final class AudioUi461Checks {
  private static final Path REPORT = Path.of("power-beta-data/reports/release-461/audio-ui");
  private static final int[][] SIZES = {{854, 480}, {320, 240}};
  private static final String FIRST = "music:calm1.ogg", SECOND = "music:calm2.ogg", THIRD = "music:creative1.ogg";
  private static final String QUIET = "ui461-quiet";
  private static final List<String> CAPTURES = List.of("options", "all-sounds", "global-sound-search", "library", "library-search",
      "playlists", "playlist-search", "editor", "grouped", "add-songs", "add-songs-menu", "readonly-beta", "favorites",
      "song-hover", "song-context", "playlist-picker", "menu-playlist-picker", "folder-chooser", "new-playlist", "scrubber", "playlist-error", "grouped-queue", "add-songs-queue");

  private AudioUi461Checks() {}

  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("capture-rules")) { ruleTabs(mc); return; }
    if (action.equals("capture-folders")) { folderRows(mc); return; }
    check(List.of("check", "capture").contains(action), "use audio-ui-461 check|capture|capture-rules");
    SilentAudioChecks.ready(mc);
    check(mc.world != null && mc.player != null && !mc.world.isRemote, "open a disposable singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable client");
    failures = 0;
    try (var state = new State(mc)) {
      if (action.equals("capture")) capture(mc, state);
      else {
        test("AUDIO_UI461 ordinary Audio, All sounds and global search retain their own scopes", () -> scopes(mc, state));
        test("AUDIO_UI461 alias search preserves song order and does not fuzzy-match", () -> search(mc, state));
        test("AUDIO_UI461 Beta stays read-only and playlist Save/Cancel own an isolated draft", () -> drafts(mc, state));
        test("AUDIO_UI461 context, favorite buttons and exact-entry sliders retain their input targets", () -> controls(mc, state));
        test("AUDIO_UI461 grouped and filtered views protect playback order", () -> ordering(mc, state));
        test("AUDIO_UI461 toolbar pickers share actions, selection and responsive bounds", () -> pickers(mc, state));
        test("AUDIO_UI461 native scrubber keeps its endpoints and equal side margins", () -> scrubber(mc, state));
      }
    }
    log("AUDIO_UI461 " + action + " failures=" + failures);
    check(failures == 0, "audio-ui-461 failures=" + failures);
  }

  private static AudioSettings seed() {
    var settings = new AudioSettings();
    settings.playlists.add(new MusicPlaylist(QUIET, "Quiet afternoons", List.of(SECOND, FIRST, THIRD), Map.of("Alpha", 40)));
    settings.playlists.add(new MusicPlaylist("ui461-building", "Building", List.of(THIRD, FIRST), Map.of()));
    settings.favourites.add(FIRST); settings.sounds.put(FIRST, 35);
    MusicPlaylist.load(settings, QUIET); MusicPlaylist.loadMenu(settings, MusicPlaylist.SAME_AS_WORLD);
    return settings;
  }

  private static void scopes(Minecraft mc, State state) throws Exception {
    var options = state.open(854, 480);
    check(settingsInRows(options).stream().noneMatch(AudioUi461Checks::individualSound), "ordinary Audio expands individual game sounds");
    invoke(options, "openAllSounds");
    check((boolean)get(options, "allSounds"), "All sounds did not open its scope");
    var sounds = settingsInRows(options).stream().filter(AudioUi461Checks::individualSound).toList();
    check(!sounds.isEmpty(), "All sounds has no individual sound rows");
    check(settingsInRows(options).stream().noneMatch(setting -> setting.id.startsWith("audio.sound.music:")), "All sounds contains music tracks");
    ((TextInput)get(options, "search")).setText("click"); invoke(options, "layout");
    check((boolean)get(options, "allSounds"), "typing left the All sounds scope");
    check(settingsInRows(options).stream().allMatch(AudioUi461Checks::individualSound), "local sound search escaped its scope");
    set(options, "allSounds", false); set(options, "page", "General");
    setStatic(Class.forName("local.luke.power.ui.OptionsSearchScope"), "choice", false);
    ((TextInput)get(options, "search")).setText("click"); invoke(options, "layout");
    var rows = (List<?>)get(options, "rows");
    check(settingsInRows(options).stream().anyMatch(AudioUi461Checks::individualSound), "global search cannot discover individual sounds");
    check(rows.stream().anyMatch(row -> {
      try { return String.valueOf(invoke(row, "group")).contains("Audio / Game sounds / All"); }
      catch (Exception failure) { throw new RuntimeException(failure); }
    }), "global sound results lost the Audio / Game sounds / All breadcrumb");
  }

  private static boolean individualSound(Setting setting) {
    return setting.id.startsWith("audio.sound.") && !setting.id.startsWith("audio.sound.music:");
  }
  private static List<Setting> settingsInRows(PowerOptionsScreen options) throws Exception {
    var settings = new ArrayList<Setting>();
    for (Object row : (List<?>)get(options, "rows")) { var setting = (Setting)invoke(row, "setting"); if (setting != null) settings.add(setting); }
    return settings;
  }

  private static void search(Minecraft mc, State state) throws Exception {
    var options = state.open(854, 480); var library = library(options);
    library.showTracks(); expand(library);
    List<String> source = tracks(library);
    query(library, "mine");
    List<String> expected = source.stream().filter(id -> {
      var track = BuiltinMusic.find(id); return track != null && track.title().toLowerCase(Locale.ROOT).contains("mine");
    }).toList();
    check(!expected.isEmpty() && tracks(library).equals(expected), "alias substring search changed alphabetical order or searched hidden metadata");
    query(library, "mnecraft"); check(tracks(library).isEmpty(), "song search still uses fuzzy matching");
    library.openPresets(); query(library, "quiet");
    var presets = (List<?>)invoke(get(library, "presets"), "filteredPresets");
    check(presets.size() == 1 && ((MusicPlaylist)presets.get(0)).id().equals(QUIET), "playlist catalog search did not filter names");
    query(library, "minecraft");
    check(((List<?>)invoke(get(library, "presets"), "filteredPresets")).isEmpty(), "playlist search matched song membership");
    library.showTracks(); check(library.query().isEmpty(), "tab navigation retained the previous tab's query");
  }

  private static void drafts(Minecraft mc, State state) throws Exception {
    var options = state.open(854, 480); var library = library(options);
    edit(options, library, MusicPlaylist.BETA);
    Object panel = get(library, "presets"); var beta = draft(library);
    check(beta.readOnly() && !(boolean)invoke(library, "playlistEditable"), "Beta opened an editable draft");
    check(!(boolean)invoke(library, "canOrderTracks"), "read-only Beta owns track reordering");
    boolean blocked = false;
    try { beta.include(FIRST, false); } catch (IllegalArgumentException | IllegalStateException expected) { blocked = true; }
    check(blocked && beta.tracks().equals(BuiltinMusic.VANILLA), "Beta mutation guard lost its original tracks");
    invoke(library, "cancelPlaylist");
    edit(options, library, QUIET); var original = MusicPlaylist.find(AudioConfig.current(), QUIET);
    check(!(boolean)invoke(library, "playlistDirty"), "an unchanged playlist enables Save");
    draft(library).include(SECOND, false); invoke(library, "syncPanel");
    check((boolean)invoke(library, "playlistDirty"), "a changed playlist leaves Save disabled");
    check(MusicPlaylist.find(AudioConfig.current(), QUIET).tracks().equals(original.tracks()), "unsaved membership changed playback");
    invoke(library, "cancelPlaylist");
    check(MusicPlaylist.find(AudioConfig.current(), QUIET).tracks().equals(original.tracks()), "playlist Cancel changed saved membership");
    edit(options, library, QUIET); draft(library).include(SECOND, false); invoke(library, "syncPanel");
    savePlaylist(options);
    check(!MusicPlaylist.find(AudioConfig.current(), QUIET).tracks().contains(SECOND), "footer Save playlist lost the draft");
    check(AudioConfig.current().playlist.equals(QUIET), "playlist Save changed the active identity");
    options.session().discard();
    check(MusicPlaylist.find(AudioConfig.current(), QUIET).tracks().equals(original.tracks()), "Options Discard failed to restore the playlist Save preview");
    library.openPresets(); invoke(get(library, "presets"), "newPlaylist"); invoke(library, "syncPanel");
    check(draft(library).creating() && draft(library).tracks().isEmpty(), "new playlist copied the current membership");
    options = state.open(320, 240); library = library(options); playlistError(options, library);
    check(!(boolean)invoke(library, "showMusicStatus"), "small error fixture did not wrap its toolbar");
    check(!((String)invoke(library, "footerError")).isEmpty(), "small playlist failure has no visible footer route");
    check((boolean)invoke(library, "playlistEditor"), "duplicate name closed the draft");
  }

  private static void controls(Minecraft mc, State state) throws Exception {
    var options = state.open(854, 480); var library = library(options); library.showTrack(FIRST);
    int y = trackY(library, FIRST); var layout = (MusicRowLayout)invoke(library, "rowLayout");
    click(options, layout.volumeX() + layout.volumeWidth() - 1, y + 8, 1);
    check((boolean)invoke(get(options, "valueEditor"), "active"), "right slider edge missed exact entry");
    ((TextInput)get(get(options, "valueEditor"), "input")).setText("71"); key(options, Keyboard.KEY_RETURN);
    check(AudioConfig.current().sounds.get(FIRST) == 71, "exact song volume did not commit");
    int x = layout.favouriteX() + layout.buttonWidth() / 2;
    click(options, x, y + 8, 0);
    check(!AudioConfig.current().favourites.contains(FIRST), "favorite star did not toggle its song");
    context(options, library, FIRST);
    var choices = get(options, "choices");
    check(get(choices, "labels").equals(List.of("Add to queue", "Favourite", "Preview")), "song context has stale or duplicated actions");
    check((boolean)get(choices, "context"), "song actions draw a bordered dropdown");
    key(options, Keyboard.KEY_ESCAPE);
    for (double fraction : new double[] {0, 1}) {
      int edge = layout.volumeX() + (fraction == 0 ? 0 : layout.volumeWidth() - 1);
      click(options, edge, y + 8, 0); release(options, edge, y + 8, 0);
      check(AudioConfig.current().sounds.getOrDefault(FIRST, 100) == (fraction == 0 ? 0 : 100), "slider endpoint does not match its visible edge");
    }
  }

  private static void ordering(Minecraft mc, State state) throws Exception {
    var options = state.open(854, 480); var library = library(options); edit(options, library, QUIET);
    var draft = draft(library); var original = draft.tracks();
    query(library, "minecraft");
    int y = trackY(library, FIRST), x = number(library, "left") + 24;
    click(options, x, y + 8, 0);
    check(!(boolean)invoke(get(library, "reorder"), "active"), "filtered playlist admits track reordering");
    release(options, x, y + 26, 0); check(draft.tracks().equals(original), "filtered release changed full playback order");
    query(library, ""); Object panel = get(library, "presets");
    click(options, number(panel, "groupedX") + 8, number(panel, "actionY") + 8, 0); expand(library);
    check(draft.tracks().equals(original) && !(boolean)invoke(library, "canOrderTracks"), "Grouped changed or admits playback ordering");
    var layout = (MusicRowLayout)invoke(library, "rowLayout");
    int groupRight = number(library, "groupVolumeX") + number(library, "groupVolumeWidth");
    check(groupRight == layout.volumeX() + layout.volumeWidth()
        && number(library, "groupVolumeX") == layout.volumeX(), "group slider does not align with child sliders");
    check(number(library, "groupHeight", "Alpha") == 20, "playlist group heading is taller than normal settings headings");
    check(draft.volume("Alpha") == 40, "changing view replaced the playlist's group gain");
    for (int queued : new int[]{0, 1}) {
      MusicRequests.edit(queue -> { queue.tracks.clear(); if (queued > 0) queue.add(FIRST); });
      for (String view : List.of("grouped", "add-songs")) {
        var small = state.open(320, 240);
        stage(mc, state, small, view, new int[]{320, 240});
        var smallLibrary = (MusicLibraryScreen)get(small, "library");
        check(number(smallLibrary, "bottom") - number(smallLibrary, "listTop") >= 44,
            "small " + view + " cannot show a heading and full song controls; queue=" + queued);
      }
    }
    options.session().discard();
  }

  private static void pickers(Minecraft mc, State state) throws Exception {
    for (int[] size : SIZES) {
      var options = state.open(size[0], size[1]); var library = library(options);
      @SuppressWarnings("unchecked") var buttons = (List<AudioToolbar.Button>)invoke(options, "musicButtons");
      var shuffle = buttons.stream().filter(button -> button.action() == AudioToolbar.Action.SHUFFLE).findFirst().orElseThrow();
      var playlist = buttons.stream().filter(button -> button.action() == AudioToolbar.Action.PLAYLIST).findFirst().orElseThrow();
      check(shuffle.x() == number(options, "contentRight") - 20 && shuffle.y() == playlist.y(), "Shuffle moved away from the playlist selector");
      for (var button : buttons) {
        check(button.x() >= number(options, "left") && button.x() + button.width() <= number(options, "contentRight"), "audio toolbar button escaped the content area");
        check(mc.textRenderer.getWidth(button.label()) <= button.width() - 4, "audio toolbar label is clipped");
      }
      String world = AudioConfig.current().playlist;
      click(options, playlist.x() + 8, playlist.y() + 8, 0);
      Object popup = get(options, "choices"); var labels = (List<?>)get(popup, "labels");
      check(labels.get(0).equals("Edit playlists") && labels.contains("Beta") && labels.contains("Quiet afternoons"), "toolbar picker lacks the shared editor action");
      check((int)get(popup, "selected") == labels.indexOf("Quiet afternoons"), "picker does not mark the active world playlist");
      check((int)get(popup, "defaultIndex") == labels.indexOf("Beta"), "picker lost the selected Defaults base target");
      choose(options, 0); check(library.presetVisible() && AudioConfig.current().playlist.equals(world), "Edit playlists selected a playlist");
    }
  }

  private static void scrubber(Minecraft mc, State state) throws Exception {
    state.open(854, 480);
    state.playback();
    var settings = AudioConfig.copy(); settings.menuControls = settings.menuControlsScrub = true; AudioConfig.preview(settings);
    var pause = new net.minecraft.class_525(); mc.setScreen(pause); pause.init(mc, 854, 480); pause.render(-1, -1, 0);
    var music = (PauseMenuMusic)get(pause, "power$music"); Object status = get(music, "scrub");
    int x = ((Number)get(status, "barX")).intValue(), width = ((Number)get(status, "barWidth")).intValue();
    int y = ((Number)get(status, "top")).intValue();
    check((boolean)get(status, "available"), "native track duration did not enable the scrubber");
    int left = music.bounds().x(), total = music.bounds().width();
    check(x - left == left + total - x - width, "standalone scrubber has unequal side margins");
    check(!(boolean)invoke(status, "press", new Class<?>[] {int.class, int.class, int.class}, x - 1, y + 8, 0), "scrubber claims outside its left edge");
    check((boolean)invoke(status, "press", new Class<?>[] {int.class, int.class, int.class}, x, y + 8, 0), "scrubber misses its left edge");
    check(((Number)get(status, "fraction")).doubleValue() == 0, "scrubber left edge cannot select zero"); invoke(status, "cancel");
    check((boolean)invoke(status, "press", new Class<?>[] {int.class, int.class, int.class}, x + width - 1, y + 8, 0), "scrubber misses its right edge");
    check(((Number)get(status, "fraction")).doubleValue() == 1, "scrubber right edge cannot select the full duration"); invoke(status, "cancel");
  }

  private static void capture(Minecraft mc, State state) throws Exception {
    Files.createDirectories(REPORT); var files = new ArrayList<String>();
    var nativeScale = Class.forName("local.luke.power.controls.util.ModOptions").getField("realGuiScale");
    int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE); GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      for (int[] size : SIZES) for (String view : CAPTURES) {
        if (!view.endsWith("-queue") && !view.equals("playlist-error") && !MusicRequests.tracks().isEmpty())
          MusicRequests.edit(queue -> queue.tracks.clear());
        var options = state.open(size[0], size[1]);
        Screen screen = stage(mc, state, options, view, size);
        String filename = view + "-" + size[0] + ".png";
        int mx = -1, my = -1;
        if (view.equals("song-hover")) {
          var library = (MusicLibraryScreen)get(options, "library"); mx = number(library, "left") + 30; my = trackY(library, FIRST) + 8;
        }
        float previousScale = nativeScale.getFloat(null);
        try {
          nativeScale.setFloat(null, .25f);
          SettingsSnapshot.render(mc, screen, size[0], size[1], REPORT.resolve(filename), mx, my);
        } finally { nativeScale.setFloat(null, previousScale); }
        files.add(filename);
      }
    } finally {
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glPopAttrib(); GL11.glMatrixMode(mode);
    }
    Files.writeString(REPORT.resolve("captures.json"), Catalog.JSON.toJson(files));
    log("AUDIO_UI461 native captures=" + files.size() + " directory=" + REPORT.toAbsolutePath());
  }

  /** Run after music-playlists-460 prepare has finished its asynchronous scan. */
  private static void folderRows(Minecraft mc) throws Exception {
    SilentAudioChecks.ready(mc);
    check(mc.world != null && !AudioConfig.current().musicDirectories.isEmpty(), "prepare disposable folder imports first");
    Files.createDirectories(REPORT);
    Screen previous = mc.currentScreen;
    int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE); GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      for (int[] size : SIZES) {
        var options = MusicPlaylists460Checks.stage(mc, "library", size[0], size[1]);
        var library = (MusicLibraryScreen)get(options, "library");
        for (String path : AudioConfig.current().musicDirectories)
          check(number(library, "groupHeight", MusicLibrarySources.id(path)) == 20, "imported folder row height differs");
        SettingsSnapshot.render(mc, options, size[0], size[1], REPORT.resolve("library-folders-" + size[0] + ".png"));
        options.session().discard(); options.removed();
      }
    } finally {
      mc.setScreen(previous);
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glPopAttrib(); GL11.glMatrixMode(mode);
    }
    log("AUDIO_UI461 folder heading captures=2");
  }

  private static void ruleTabs(Minecraft mc) throws Exception {
    check(mc.world == null, "return to the title before capturing creation-default tabs");
    Files.createDirectories(REPORT);
    Screen previous = mc.currentScreen;
    boolean automatic = MenuPreferences.current().autoApply;
    int matrix = GL11.glGetInteger(GL11.GL_MATRIX_MODE); GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    PowerOptionsScreen options = null;
    try {
      MenuPreferences.current().autoApply = false;
      options = new PowerOptionsScreen(previous); mc.setScreen(options);
      set(options, "page", "Game rules"); set(options, "libraryOpen", false); set(options, "allSounds", false);
      ((TextInput)get(options, "search")).setText(""); ((Set<?>)get(options, "collapsed")).clear();
      for (int[] size : SIZES) {
        options.init(mc, size[0], size[1]); invoke(options, "layout");
        check((boolean)invoke(options, "rulesToolbar"), "native main-menu creation-default tabs are unavailable");
        for (int tab = 0; tab < 2; tab++) {
          click(options, number(options, "left") + tab * (number(options, "ruleButtonWidth") + 4) + 8, 61, 0);
          check((boolean)get(options, "cheatPreset") == (tab == 1), "creation-default tab click did not switch scope");
          SettingsSnapshot.render(mc, options, size[0], size[1], REPORT.resolve("rules-" + (tab == 0 ? "survival" : "cheats") + "-" + size[0] + ".png"));
        }
      }
    } finally {
      if (options != null) options.session().discard();
      MenuPreferences.current().autoApply = automatic; mc.setScreen(previous);
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glPopAttrib(); GL11.glMatrixMode(matrix);
    }
    log("AUDIO_UI461 native creation-default tab captures=4 directory=" + REPORT.toAbsolutePath());
  }

  private static Screen stage(Minecraft mc, State state, PowerOptionsScreen options, String view, int[] size) throws Exception {
    if (view.equals("options")) return options;
    if (view.equals("all-sounds")) { invoke(options, "openAllSounds"); return options; }
    if (view.equals("global-sound-search")) {
      set(options, "page", "General"); ((TextInput)get(options, "search")).setText("click"); invoke(options, "layout"); return options;
    }
    if (view.equals("menu-playlist-picker")) { settingPicker(options, "audio.menuPlaylist"); return options; }
    var library = library(options);
    switch (view) {
      case "library" -> { library.showTracks(); expand(library); }
      case "library-search" -> { library.showTracks(); query(library, "mine"); }
      case "playlists" -> library.openPresets();
      case "new-playlist" -> { library.openPresets(); invoke(get(library, "presets"), "newPlaylist"); invoke(library, "syncPanel"); }
      case "playlist-search" -> { library.openPresets(); query(library, "quiet"); }
      case "readonly-beta" -> edit(options, library, MusicPlaylist.BETA);
      case "favorites" -> invoke(library, "selectFilter", new Class<?>[] {String.class}, "favorites");
      case "editor", "grouped", "add-songs", "add-songs-menu", "grouped-queue", "add-songs-queue" -> {
        if (view.endsWith("-queue")) MusicRequests.edit(queue -> { queue.tracks.clear(); queue.add(FIRST); });
        edit(options, library, QUIET); Object panel = get(library, "presets");
        if (view.startsWith("grouped")) { click(options, number(panel, "groupedX") + 8, number(panel, "actionY") + 8, 0); expand(library); }
        if (view.startsWith("add-songs")) {
          click(options, number(panel, "addX") + 8, number(panel, "actionY") + 8, 0);
          if (view.equals("add-songs") || view.equals("add-songs-queue")) { choose(options, 0); expand(library); }
        }
      }
      case "playlist-error" -> playlistError(options, library);
      case "song-hover" -> library.showTrack(FIRST);
      case "scrubber" -> {
        state.playback(); var settings = AudioConfig.copy(); settings.menuControls = settings.menuControlsScrub = true; AudioConfig.preview(settings);
        var pause = new net.minecraft.class_525(); mc.setScreen(pause); pause.init(mc, size[0], size[1]); return pause;
      }
      case "song-context" -> context(options, library, FIRST);
      case "playlist-picker" -> {
        @SuppressWarnings("unchecked") var buttons = (List<AudioToolbar.Button>)invoke(options, "musicButtons");
        var button = buttons.stream().filter(candidate -> candidate.action() == AudioToolbar.Action.PLAYLIST).findFirst().orElseThrow();
        click(options, button.x() + 8, button.y() + 8, 0);
      }
      case "folder-chooser" -> {
        var picker = new DirectoryScreen(options, values -> {}, List.of());
        set(picker, "directory", state.directory); invoke(picker, "read"); mc.setScreen(picker); picker.init(mc, size[0], size[1]); return picker;
      }
      default -> throw new IllegalArgumentException(view);
    }
    return options;
  }

  private static void playlistError(PowerOptionsScreen options, MusicLibraryScreen library) throws Exception {
    MusicRequests.edit(queue -> { queue.tracks.clear(); queue.add(FIRST); });
    invoke(options, "layout");
    edit(options, library, QUIET);
    ((TextInput)get(get(library, "presets"), "name")).setText("Building");
    savePlaylist(options);
    check(!((String)invoke(get(library, "presets"), "error")).isEmpty(), "duplicate name was accepted");
  }

  private static MusicLibraryScreen library(PowerOptionsScreen options) throws Exception {
    invoke(options, "editMusic"); var library = (MusicLibraryScreen)get(options, "library");
    check(library != null && (boolean)invoke(options, "libraryVisible"), "Music Edit did not open the library"); return library;
  }
  private static void edit(PowerOptionsScreen options, MusicLibraryScreen library, String id) throws Exception {
    library.openPresets(); Object panel = get(library, "presets");
    var values = (List<?>)invoke(panel, "filteredPresets");
    int index = 0; while (index < values.size() && !((MusicPlaylist)values.get(index)).id().equals(id)) index++;
    check(index < values.size(), "missing fixture playlist " + id);
    int row = number(panel, "rowTop") + index * number(panel, "rowHeight");
    var track = (ScrollBar.Track)invoke(panel, "track"); set(panel, "scroll", Math.min((int)track.maximum(), index * number(panel, "rowHeight")));
    int y = ((Number)invoke(panel, "actionsY", new Class<?>[] {int.class}, row - (int)get(panel, "scroll"))).intValue();
    click(options, number(panel, "editX") + 8, y + 8, 0);
    check((boolean)invoke(panel, "editing") && draft(library).id().equals(id), "View/Edit missed playlist " + id);
  }
  private static void settingPicker(PowerOptionsScreen options, String id) throws Exception {
    var row = ((List<?>)get(options, "rows")).stream().filter(candidate -> {
      try { var setting = (Setting)invoke(candidate, "setting"); return setting != null && setting.id.equals(id); }
      catch (Exception failure) { throw new RuntimeException(failure); }
    }).findFirst().orElseThrow();
    set(options, "scroll", Math.max(0d, ((Number)invoke(row, "y")).doubleValue() - 24)); invoke(options, "layout");
    int x = ((Number)invoke(options, "mainControlLeft", new Class<?>[] {row.getClass()}, row)).intValue();
    int offset = (int)invokeStatic(Class.forName("local.luke.power.ui.SettingRows"), "buttonOffset", new Class<?>[] {int.class}, number(options, "span"));
    int y = number(options, "top") + ((Number)invoke(row, "y")).intValue() - ((Number)get(options, "scroll")).intValue() + offset;
    click(options, x + 8, y + 8, 0);
    check((boolean)invoke(get(options, "choices"), "active"), "playlist setting picker did not open");
  }
  static void savePlaylist(PowerOptionsScreen options) throws Exception {
    for (Object button : (List<?>)invoke(options, "draftFooter"))
      if (invoke(button, "label").equals("Save playlist")) {
        check((boolean)invoke(button, "enabled"), "playlist Save is disabled for a changed draft");
        click(options, ((Number)invoke(button, "x")).intValue() + 8, options.height - 20, 0); return;
      }
    throw new AssertionError("playlist editor has no Save playlist footer action");
  }
  static int catalogActionY(Object panel, int index) throws Exception {
    int y = number(panel, "rowTop") + index * number(panel, "rowHeight") - ((Number)get(panel, "scroll")).intValue();
    return ((Number)invoke(panel, "actionsY", new Class<?>[] {int.class}, y)).intValue();
  }
  private static void context(PowerOptionsScreen options, MusicLibraryScreen library, String id) throws Exception {
    library.showTrack(id); int y = trackY(library, id); click(options, number(library, "left") + 30, y + 8, 1);
    check((boolean)invoke(get(options, "choices"), "active"), "song context did not open");
  }
  private static int trackY(MusicLibraryScreen library, String id) throws Exception {
    for (Object row : (List<?>)get(library, "rows")) if (id.equals(invoke(row, "id"))) {
      int natural = ((Number)invoke(row, "y")).intValue();
      invoke(library, "scroll", new Class<?>[] {int.class}, Math.max(0, natural - 20));
      return number(library, "listTop") + natural - number(library, "scroll");
    }
    throw new AssertionError("song row is missing " + id);
  }
  private static MusicPresetDraft draft(MusicLibraryScreen library) throws Exception { return (MusicPresetDraft)invoke(get(library, "presets"), "draft"); }
  @SuppressWarnings("unchecked") private static List<String> tracks(MusicLibraryScreen library) throws Exception { return (List<String>)get(library, "tracks"); }
  private static void query(MusicLibraryScreen library, String value) throws Exception { ((TextInput)get(library, "search")).setText(value); invoke(library, "rebuild"); }
  private static void expand(MusicLibraryScreen library) throws Exception { ((Set<?>)get(library, "collapsed")).clear(); invoke(library, "rebuildRows"); }
  private static void choose(PowerOptionsScreen options, int index) { key(options, Keyboard.KEY_HOME); for (int i = 0; i < index; i++) key(options, Keyboard.KEY_DOWN); key(options, Keyboard.KEY_RETURN); }
  private static void click(PowerOptionsScreen options, int x, int y, int button) { ((ScreenInput)(Object)options).power$click(x, y, button); }
  private static void release(PowerOptionsScreen options, int x, int y, int button) { ((ScreenInput)(Object)options).power$release(x, y, button); }
  private static void key(PowerOptionsScreen options, int key) { ((ScreenInput)(Object)options).power$key('\0', key); }
  private static int indexed(Object owner, String name, int index) throws Exception { return ((Number)invoke(owner, name, new Class<?>[] {int.class}, index)).intValue(); }
  private static int number(Object owner, String name) throws Exception { return ((Number)invoke(owner, name)).intValue(); }
  private static int number(Object owner, String name, String value) throws Exception { return ((Number)invoke(owner, name, new Class<?>[] {String.class}, value)).intValue(); }
  private static Field field(Class<?> type, String name) throws Exception {
    for (; type != null; type = type.getSuperclass()) try { var field = type.getDeclaredField(name); field.setAccessible(true); return field; } catch (NoSuchFieldException ignored) {}
    throw new NoSuchFieldException(name);
  }
  private static Object get(Object owner, String name) throws Exception { return field(owner.getClass(), name).get(owner); }
  private static void set(Object owner, String name, Object value) throws Exception { field(owner.getClass(), name).set(owner, value); }
  private static void setStatic(Class<?> type, String name, Object value) throws Exception { field(type, name).set(null, value); }
  private static Object invoke(Object owner, String name) throws Exception { return invoke(owner, name, new Class<?>[0]); }
  private static Object invoke(Object owner, String name, Class<?>[] types, Object... args) throws Exception { return method(owner.getClass(), name, types).invoke(owner, args); }
  private static Object invokeStatic(Class<?> type, String name, Class<?>[] types, Object... args) throws Exception { return method(type, name, types).invoke(null, args); }
  private static Method method(Class<?> type, String name, Class<?>[] types) throws Exception {
    for (; type != null; type = type.getSuperclass()) try { var method = type.getDeclaredMethod(name, types); method.setAccessible(true); return method; } catch (NoSuchMethodException ignored) {}
    throw new NoSuchMethodException(name);
  }

  private static final class State implements AutoCloseable {
    private final Minecraft mc;
    private final MusicPlaylists460Checks.State previous;
    private final boolean automatic, grabbed;
    private final Object searchScope;
    private final List<PowerOptionsScreen> opened = new ArrayList<>();
    private boolean ownsPlayback;
    final Path directory;
    State(Minecraft mc) throws Exception {
      this.mc = mc; previous = new MusicPlaylists460Checks.State(mc); automatic = MenuPreferences.current().autoApply;
      grabbed = Mouse.isGrabbed(); searchScope = field(Class.forName("local.luke.power.ui.OptionsSearchScope"), "choice").get(null);
      directory = Files.createTempDirectory("audio-ui-461-"); Files.createDirectories(directory.resolve("Building music")); Files.createDirectories(directory.resolve("Quiet music"));
      MenuPreferences.current().autoApply = false; Mouse.setGrabbed(false);
    }
    PowerOptionsScreen open(int width, int height) throws Exception {
      for (var options : opened) { options.session().discard(); options.removed(); } opened.clear();
      AudioConfig.preview(seed());
      setStatic(Class.forName("local.luke.power.ui.OptionsSearchScope"), "choice", false);
      var options = new PowerOptionsScreen(mc.currentScreen); mc.setScreen(options); options.init(mc, width, height);
      set(options, "page", "Audio"); set(options, "libraryOpen", false); set(options, "allSounds", false);
      ((TextInput)get(options, "search")).setText(""); ((Set<?>)get(options, "collapsed")).clear(); invoke(options, "layout");
      opened.add(options); return options;
    }
    void playback() throws Exception {
      if (!ownsPlayback) { AudioController.playNow(FIRST); AudioController.pause(); ownsPlayback = true; }
      long deadline = System.nanoTime() + 3_000_000_000L;
      while (AudioController.duration() <= 0 && System.nanoTime() < deadline) Thread.sleep(5);
      check(AudioController.duration() > 0, "native bundled track duration was not ready");
    }
    public void close() throws Exception {
      try {
        for (var options : opened) { options.session().discard(); options.removed(); }
        if (ownsPlayback) AudioController.quiet();
        MenuPreferences.current().autoApply = automatic; setStatic(Class.forName("local.luke.power.ui.OptionsSearchScope"), "choice", searchScope);
        previous.close(); Mouse.setGrabbed(grabbed);
      } finally { try (var paths = Files.walk(directory)) { for (var path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path); } }
    }
  }
}
