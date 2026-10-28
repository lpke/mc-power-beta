package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.ToIntFunction;
import javax.imageio.ImageIO;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

/** Native 4.6.6 evidence and acceptance checks. Never imports or plays audio. */
final class Release466UiChecks {
  private static final Path REPORT = Path.of("power-beta-data/reports/release-466/ui");
  private static final int[][] SIZES = {{854, 480}, {320, 240}};
  private static final String CUSTOM = "ui466-building", NAME = "Building sessions", ACTIVE = "ui466-active";
  private static final String FIRST = "music:calm1.ogg", SECOND = "music:calm2.ogg";
  private static final String CONSEQUENCE = "Files and playlist entries stay unchanged. Tracks unavailable from remaining imports cannot play again until reimported.";
  private static String source, group;
  private static final List<Map<String, Object>> captures = new ArrayList<>(), observations = new ArrayList<>();

  private Release466UiChecks() {}

  static void run(Minecraft mc, String action) throws Exception {
    check(Set.of("capture", "check").contains(action), "use release-466-ui capture|check");
    SilentAudioChecks.ready(mc);
    check(mc.world != null && mc.player != null && !mc.world.isRemote, "open a disposable singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable client");
    check(Display.isActive(), "focus the disposable client");
    check(Display.getWidth() >= 1708 && Display.getHeight() >= 960, "resize disposable display to at least 1708x960");
    check(AudioController.currentTrackId().isEmpty(), "start with idle disposable music");
    Files.createDirectories(REPORT.resolve("probes")); captures.clear(); observations.clear(); failures = 0;
    try (var sources = new SourceState(); var state = new Release462Checks.State(mc)) {
      // Actual screens are written before any release acceptance checks run.
      capture(mc, state);
      if (action.equals("check")) {
        test("RELEASE466 UI direct Active/Favorites have no local title Back", () -> roots(mc, state));
        test("RELEASE466 UI Playlists remembers nested state across tabs and resets on repeated selection", () -> navigation(mc, state));
        test("RELEASE466 UI dirty tab exits retain Save Discard Keep and Add state", () -> dirtyNavigation(mc, state));
        test("RELEASE466 UI new Save ID and deleted remembered playlists restore safely", () -> createdAndRemoved(mc, state));
        test("RELEASE466 UI top Audio Back clears stale parent restoration", () -> topExit(mc, state));
        test("RELEASE466 UI compact queued editor retains accessible controls and a full song row", () -> queuedCompact(mc, state));
        test("RELEASE466 UI folder count delete far-right slider geometry and tooltip ownership", () -> geometry(mc, state));
        test("RELEASE466 UI long folder warning wraps full consequence and preserves files and membership", () -> folderWarning(mc, state));
        test("RELEASE466 UI all actual volume settings reach every percent by slider cycle exact entry", () -> volumeSteps(mc, state));
        test("RELEASE466 UI manual group and track exact entry accepts 37 percent", () -> exactVolume(mc, state));
      }
    } finally {
      Files.writeString(REPORT.resolve("captures.json"), Catalog.JSON.toJson(captures));
      Files.writeString(REPORT.resolve(action + ".json"), Catalog.JSON.toJson(Map.of("action", action,
          "failures", failures, "observations", observations, "limits", List.of(
              "Synthetic folder ownership uses built-in tracks and a disposable marker file; import scanning and decoding are outside this fixture.",
              "All manifest images render actual application screens; font and tooltip reference probes are excluded.",
              "Pointer input uses the native Screen input bridge; physical desktop focus changes remain manual.",
              "The 101-position volume matrix changes isolated Setting values without preview callbacks or disk writes."))));
    }
    log("RELEASE466 UI " + action + " failures=" + failures + " actualCaptures=" + captures.size());
    check(failures == 0, "release-466-ui failures=" + failures);
  }

  private static PowerOptionsScreen open(Release462Checks.State state, int[] size) throws Exception {
    if (state.options != null) { state.options.session().discard(); state.options.removed(); }
    var audio = new AudioSettings();
    List<String> tracks = new ArrayList<>(MusicPlaylist.beta().tracks());
    tracks.add("music:creative1.ogg"); tracks.add("music:creative2.ogg");
    audio.playlists.add(new MusicPlaylist(CUSTOM, NAME, tracks));
    audio.playlists.add(new MusicPlaylist(ACTIVE, "Current building playlist", List.of(FIRST, SECOND)));
    for (int i = 0; i < 14; i++) audio.playlists.add(new MusicPlaylist("ui466-list-" + i,
        "Building playlist " + i, tracks));
    audio.favourites.add(FIRST); audio.sounds.put(FIRST, 35);
    audio.musicDirectories.add(source); audio.libraryGroupVolumes.put(group, 40);
    MusicPlaylist.load(audio, ACTIVE); MusicPlaylist.loadMenu(audio, MusicPlaylist.SAME_AS_WORLD);
    AudioConfig.preview(audio);
    set(AudioController.class, "sourceLibrary", new MusicLibrarySources.Snapshot(
        List.of(new MusicLibrarySources.Folder(group, source, "Building music folder", "Ready", List.of(FIRST, SECOND), List.of())),
        Map.of(FIRST, group, SECOND, group), List.of()));
    set(AudioController.class, "rotationConfig", AudioConfig.current()); set(AudioController.class, "enabledTracks", List.of());
    MusicRequests.edit(queue -> queue.tracks.clear());
    var options = new PowerOptionsScreen(state.mc.currentScreen); state.options = options;
    state.mc.setScreen(options); options.init(state.mc, size[0], size[1]);
    set(options, "page", "Audio"); set(options, "libraryOpen", false); set(options, "allSounds", false);
    set(options, "conflictIds", List.of()); set(options, "changedOnly", false);
    ((Deque<?>)get(options, "history")).clear();
    ((TextInput)get(options, "search")).setText(""); ((Set<?>)get(options, "collapsed")).clear();
    call(options, "layout"); call(options, "openMusicEditor");
    return options;
  }

  private static void capture(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) for (String view : List.of("playlists-catalog", "playlists-remembered", "active-direct",
        "favorites-direct", "nested-editor", "nested-back-tooltip", "add-browser", "add-back-tooltip",
        "library-groups", "group-heading-hover", "group-slider-hover", "group-delete-hover", "folder-warning-long",
        "volume-group-37", "volume-track-37", "dirty-exit", "queued-compact-editor", "queued-compact-scrolled", "volume-audio-37", "volume-ambient-37")) {
      var options = open(state, size); var library = library(options); int mx = -1, my = -1;
      switch (view) {
        case "playlists-catalog" -> library.openPresets();
        case "active-direct" -> tab(options, library, "active");
        case "favorites-direct" -> tab(options, library, "favorites");
        case "playlists-remembered", "nested-editor", "nested-back-tooltip", "add-browser", "add-back-tooltip", "dirty-exit", "queued-compact-editor", "queued-compact-scrolled" -> {
          if (view.startsWith("queued-compact")) { MusicRequests.edit(queue -> queue.tracks.add(FIRST)); call(options, "layout"); }
          catalogEditor(options, library, CUSTOM); groupView(options, library); query(library, "a");
          if (view.equals("playlists-remembered")) { tab(options, library, "library"); tab(options, library, "playlists"); }
          if (view.startsWith("add-")) adding(options, library);
          if (view.equals("nested-back-tooltip") || view.equals("add-back-tooltip")) {
            Object header = call(panel(library), "header"); mx = n(header, "backX") + 6; my = n(panel(library), "top") + 7;
          }
          if (view.equals("queued-compact-scrolled")) { expand(library); library.wheel(-120); }
          if (view.equals("dirty-exit")) { draft(library).include(SECOND, false); call(library, "syncPanel"); topBack(options); }
        }
        case "volume-audio-37", "volume-ambient-37" -> {
          call(options, "closeLibrary");
          for (Setting value : options.session().settings()) if (VolumeControls.accepts(value)) value.parse("37");
          if (view.equals("volume-ambient-37")) {
            call(options, "openAllSounds");
            ((TextInput)get(options, "search")).setText("ambience");
          }
          call(options, "layout");
        }
        default -> {
          expand(library); int y = groupY(library, group);
          if (view.equals("group-heading-hover")) { mx = n(library, "left") + 12; my = y + 7; }
          if (view.equals("group-slider-hover") || view.equals("volume-group-37")) {
            mx = n(library, "groupVolumeX") + 8; my = y + 7;
            if (view.equals("volume-group-37")) { press(options, mx, my, 1); exact(options, "37"); }
          }
          if (view.equals("group-delete-hover")) { mx = n(library, "sourceRemoveX") + 5; my = y + 7; }
          if (view.equals("folder-warning-long")) press(options, n(library, "sourceRemoveX") + 5, y + 7, 0);
          if (view.equals("volume-track-37")) {
            int ty = trackY(library, FIRST); mx = n(library, "volumeX") + 8; my = ty + ((MusicRowLayout)call(library, "rowLayout")).controlsY() + 6;
            press(options, mx, my, 1); exact(options, "37");
          }
        }
      }
      shot(mc, options, view, size, mx, my, true);
    }
  }

  private static void roots(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) for (String id : List.of("active", "favorites")) {
      var options = open(state, size); var library = library(options); tab(options, library, id);
      Object header = call(panel(library), "header");
      check(library.playlistEditor() && !library.localBackVisible() && n(header, "backWidth") == 0,
          id + " direct tab added a local title Back");
      check(draft(library).id().equals(id.equals("active") ? ACTIVE : MusicPlaylist.FAVORITES), "wrong direct playlist identity");
      tab(options, library, "library");
      check(!library.localBackVisible(), "cross-tab Library acquired local Back history");
    }
  }

  private static void navigation(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) for (boolean add : new boolean[]{false, true}) {
      var options = open(state, size); var library = library(options); library.openPresets(); query(library, "Building");
      Object panel = panel(library); set(panel, "scroll", n(panel, "rowHeight") * 4);
      int catalogScroll = integer(panel, "scroll");
      String id = ((MusicPlaylist)((List<?>)call(panel, "filteredPresets")).get(4)).id();
      catalogRow(options, library, id); groupView(options, library); query(library, "a"); expand(library);
      call(library, "scroll", new Class<?>[]{int.class}, 24);
      int scroll = integer(library, "trackScroll");
      if (add) { adding(options, library); query(library, "Minecraft"); }
      String query = library.query(); int currentScroll = integer(library, "trackScroll");
      tab(options, library, "favorites"); check(!library.localBackVisible(), "Favorites root inherited Playlists local Back");
      tab(options, library, "library"); check(!library.localBackVisible(), "Library root inherited Playlists local Back");
      tab(options, library, "playlists");
      check(library.state().folder().equals("playlists") && library.playlistEditor() && draft(library).id().equals(id)
          && library.query().equals(query) && integer(library, "trackScroll") == currentScroll
          && (boolean)call(panel, "grouped") && (boolean)call(panel, "adding") == add,
          "returning to Playlists lost its playlist, query, scroll, grouping or Add browser");
      if (add) {
        localBack(options, library);
        check(!(boolean)call(panel, "adding") && library.query().equals("a")
            && integer(library, "trackScroll") == scroll, "Add local Back lost owning query or scroll");
      }
      Object header = call(panel, "header");
      check(n(header, "backWidth") == 18 && n(header, "backX") + 22 == n(header, "nameX"), "nested Back is not an 18px square immediately before title");
      assertTooltip(mc, options, size, "Playlists", n(header, "backX") + 6, n(panel, "top") + 7, "nested-catalog");
      localBack(options, library);
      check(!library.playlistEditor() && library.query().equals("Building") && integer(panel, "scroll") == catalogScroll,
          "same-tab Back did not restore catalog query and scroll");
      catalogRow(options, library, id); tab(options, library, "playlists");
      check(!library.playlistEditor() && !library.localBackVisible() && library.query().isEmpty(), "repeated Playlists tab did not reset to catalog root");
      observations.add(Map.of("case", "remembered-playlists", "width", size[0], "adding", add, "catalogScroll", catalogScroll,
          "editorScroll", scroll, "playlist", id));
    }
  }

  private static void dirtyNavigation(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) for (boolean add : new boolean[]{false, true}) for (int choice : new int[]{2, 1, 0}) {
      var options = open(state, size); var library = library(options); catalogEditor(options, library, CUSTOM);
      groupView(options, library); query(library, "a"); if (add) { adding(options, library); query(library, "Minecraft"); }
      var owner = draft(library); owner.include(SECOND, false); call(library, "syncPanel");
      String query = library.query(); var saved = setting(options, "audio.playlists").value.deepCopy();
      tab(options, library, "favorites");
      check(library.modal() && draft(library) == owner, "dirty tab switch bypassed draft prompt"); dirtyChoice(options, library, choice);
      if (choice == 2) {
        check(draft(library) == owner && library.playlistDirty() && library.query().equals(query)
            && (boolean)call(panel(library), "adding") == add && setting(options, "audio.playlists").value.equals(saved), "Keep editing replaced or saved isolated draft");
        continue;
      }
      check(library.state().folder().equals("favorites"), "confirmed dirty tab switch missed destination");
      tab(options, library, "playlists");
      check(draft(library).id().equals(CUSTOM) && (boolean)call(panel(library), "adding") == add
          && (boolean)call(panel(library), "grouped") && library.query().equals(query), "Save/Discard lost remembered playlist view");
      check(draft(library).included(SECOND) == (choice == 1), "Save/Discard restored wrong membership");
    }
  }

  private static void createdAndRemoved(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) for (int choice : new int[]{0, 1}) {
      var options = open(state, size); var library = library(options); library.openPresets(); call(library, "newPlaylist");
      ((TextInput)get(panel(library), "name")).setText("New building playlist"); String id = draft(library).id();
      tab(options, library, "favorites"); check(library.modal(), "new playlist exit did not prompt"); dirtyChoice(options, library, choice);
      tab(options, library, "playlists");
      check(choice == 0 ? library.playlistEditor() && draft(library).id().equals(id) && !draft(library).creating()
          : !library.playlistEditor(), "new Save ID or discarded creation did not restore safely");
    }
    for (int[] size : SIZES) {
      var options = open(state, size); var library = library(options); catalogEditor(options, library, CUSTOM);
      query(library, "a"); tab(options, library, "library");
      call(get(library, "edits"), "delete", new Class<?>[]{String.class}, CUSTOM);
      tab(options, library, "playlists");
      check(!library.playlistEditor() && library.state().folder().equals("playlists"), "removed remembered playlist did not fall back to catalog");
      check(!library.backDestination().equals(NAME), "removed playlist retained invalid Back destination");
    }
  }

  private static void topExit(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) for (int choice : new int[]{2, 1, 0}) {
      var options = open(state, size); var library = library(options); catalogEditor(options, library, CUSTOM);
      var stale = library.state(); call(options, "pushView"); set(options, "musicEditorReturn", true);
      draft(library).include(SECOND, false); call(library, "syncPanel");
      topBack(options); check(library.modal(), "dirty top Back did not prompt"); dirtyChoice(options, library, choice);
      if (choice == 2) { check(library.playlistDirty() && (boolean)call(options, "libraryVisible"), "top Keep editing exited Music"); topBack(options); dirtyChoice(options, library, 1); }
      check(!(boolean)call(options, "libraryVisible") && get(options, "page").equals("Audio")
          && get(options, "libraryState") == null && library.state().folder().equals("library") && library.query().isEmpty(), "top Audio Back retained stale Music state");
      Object navigation = get(library, "navigation");
      check(((Map<?, ?>)get(navigation, "views")).isEmpty() && ((Map<?, ?>)get(navigation, "histories")).isEmpty(), "top Audio Back retained tab views/history");
      for (Object view : (Deque<?>)get(options, "history")) check(!(boolean)call(view, "libraryOpen"), "parent history retained Music visit");
      // Reproduce a stale parent restoration after the exit; normal reopening must start clean.
      set(options, "libraryState", stale); call(options, "openMusicEditor");
      check(library.state().folder().equals("library") && library.query().isEmpty() && !library.localBackVisible(), "opening Music revived stale parent libraryState");
      topBack(options);
      check(get(options, "libraryState") == null && !(boolean)call(options, "libraryVisible"), "restoring stale pre-Music parent view survived top Audio Back");
    }
  }

  private static void queuedCompact(Minecraft mc, Release462Checks.State state) throws Exception {
    var options = open(state, SIZES[1]); var library = library(options);
    MusicRequests.edit(queue -> queue.tracks.add(FIRST)); call(options, "layout");
    catalogEditor(options, library, CUSTOM); groupView(options, library);
    Object header = call(panel(library), "header");
    check(n(library, "bottom") - n(library, "listTop") >= ((MusicRowLayout)call(library, "rowLayout")).height(), "compact queued editor lost its only complete song row");
    check(n(header, "backWidth") == 18 && n(header, "nameX") >= 0 && n(header, "nameX") + n(header, "nameWidth") <= options.width
        && n(header, "viewX") + n(header, "viewWidth") <= options.width && n(panel(library), "actionY") + 18 <= n(library, "listTop"), "compact queued title/actions escape accessible bounds");
    expand(library); library.wheel(-120);
    Object firstSong = ((List<?>)get(library, "rows")).stream().filter(row -> {
      try { return call(row, "id") != null; } catch (Exception failure) { throw new RuntimeException(failure); }
    }).findFirst().orElseThrow();
    var row = (MusicRowLayout)call(library, "rowLayout");
    int songY = n(library, "listTop") + n(firstSong, "y") - n(library, "scroll");
    check(songY >= n(library, "listTop") && songY + row.height() <= n(library, "bottom")
        && call(library, "stickyHeader") == null, "compact queued wheel did not reveal a complete song row");
    shot(mc, options, "check-queued-scrolled", SIZES[1], -1, -1, false);
    press(options, row.volumeX() + 3, songY + row.controlsY() + 6, 1);
    check((boolean)call(get(options, "valueEditor"), "active"), "compact queued song slider cannot open exact entry");
    key(options, Keyboard.KEY_ESCAPE);
    localBack(options, library);
    check(!library.playlistEditor() && library.state().folder().equals("playlists"), "compact queued local Back is inaccessible");
  }

  private static void geometry(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) {
      var options = open(state, size); var library = library(options); expand(library); int y = groupY(library, group);
      var row = (MusicRowLayout)call(library, "rowLayout"); int gx = n(library, "groupVolumeX"), gw = n(library, "groupVolumeWidth");
      check(gx + gw == n(library, "right") && gw - row.volumeWidth() == 16,
          "group slider does not finish at far right with 16px extra width");
      check(n(library, "sourceRemoveX") + n(library, "sourceRemoveWidth") + 4 == gx
          && n(library, "sourceRemoveWidth") == row.buttonWidth(), "folder Delete is not immediately before far-right slider");
      check(n(library, "countRight", new Class<?>[]{String.class}, group) < n(library, "sourceRemoveX"), "group count is not before Delete and slider");
      if (size[0] == 854) check(gw == 150 && row.volumeWidth() == 134, "wide group/song widths differ from 150/134px");
      else check(gw >= 70 && row.volumeWidth() >= 54, "compact slider widths are too narrow");
      BufferedImage actual = shot(mc, options, "check-library-geometry", size, -1, -1, false);
      textMask(mc, actual, size, "2 tracks", n(library, "countRight", new Class<?>[]{String.class}, group) - mc.textRenderer.getWidth("2 tracks"),
          y + 6, 0xaaaaaa, "group track count");
      shot(mc, options, "check-heading-hover", size, n(library, "left") + 12, y + 7, false);
      check(!library.hoverHelp().isEmpty(), "heading hover lost source tooltip");
      for (int x : new int[]{gx, gx + gw - 1, n(library, "sourceRemoveX") + 5}) {
        shot(mc, options, "check-heading-control-hover-" + x, size, x, y + 7, false);
        check(library.hoverHelp().isEmpty(), "heading tooltip covers slider or Delete");
      }
      press(options, gx + 3, y + 7, 1);
      check((boolean)call(get(options, "valueEditor"), "editing", new Class<?>[]{Object.class}, "musicGroup/" + group), "group right-click missed exact editor");
      shot(mc, options, "check-heading-exact-hover", size, gx + 10, y + 7, false);
      check(library.hoverHelp().isEmpty(), "heading tooltip covers exact-entry field"); key(options, Keyboard.KEY_ESCAPE);
      catalogEditor(options, library, CUSTOM); groupView(options, library); expand(library);
      Object heading = ((List<?>)get(library, "rows")).stream().filter(value -> {
        try { return call(value, "id") == null; } catch (Exception failure) { throw new RuntimeException(failure); }
      }).findFirst().orElseThrow();
      String headingGroup = (String)call(heading, "group"); y = groupY(library, headingGroup);
      actual = shot(mc, options, "check-playlist-group-count", size, -1, -1, false);
      int included = n(library, "includedCount", new Class<?>[]{String.class}, headingGroup);
      int total = ((Map<String, Integer>)get(library, "libraryGroupCounts")).getOrDefault(headingGroup, 0);
      String count = included + "/" + total;
      textMask(mc, actual, size, count, n(library, "countRight", new Class<?>[]{String.class}, headingGroup) - mc.textRenderer.getWidth(count), y + 6, 0xaaaaaa, "playlist grouped count");
      adding(options, library); expand(library); y = groupY(library, group);
      shot(mc, options, "check-group-add-hover", size, n(library, "right") - 20, y + 7, false);
      check(library.hoverHelp().isEmpty(), "folder heading tooltip covers Add button");
    }
  }

  private static void folderWarning(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) {
      var options = open(state, size); var library = library(options); expand(library); int y = groupY(library, group);
      var membership = setting(options, "audio.playlists").value.deepCopy();
      var favorites = setting(options, "audio.favourites").value.deepCopy();
      Path marker = Path.of(source).resolve("fixture-marker.txt"); byte[] bytes = Files.readAllBytes(marker);
      press(options, n(library, "sourceRemoveX") + 5, y + 7, 0); check(library.modal(), "Delete did not open folder warning");
      Object folders = get(library, "folders"); int buttonY = n(folders, "buttonY"), span = Math.min(n(folders, "dialogWidth"), size[0] - 24);
      List<?> lines = (List<?>)call(Class.forName("local.luke.power.ui.MusicFolderWarning"), "layout",
          new Class<?>[]{int.class, int.class, String.class, String.class, ToIntFunction.class},
          span, buttonY - 12, source, "", (ToIntFunction<String>)mc.textRenderer::getWidth);
      check(call(lines.get(0), "text").equals("Remove folder?") && n(lines.get(0), "color") == 0xffffff, "folder title is not first white line");
      List<String> body = new ArrayList<>(); boolean consequence = false; int pathLines = 0;
      BufferedImage actual = shot(mc, options, "check-folder-warning", size, -1, -1, false);
      for (int i = 0; i < lines.size(); i++) {
        Object line = lines.get(i); String text = (String)call(line, "text"); int ly = n(line, "y");
        check(mc.textRenderer.getWidth(text) <= span && ly >= 12 && ly + 8 <= buttonY - 12, "folder warning escaped compact text bounds");
        if (i > 0) {
          check(n(line, "color") == 0xaaaaaa, "folder detail is not grey");
          if (text.startsWith("Files and playlist")) consequence = true;
          if (consequence) body.add(text); else pathLines++;
        }
        textMask(mc, actual, size, text, size[0] / 2 - mc.textRenderer.getWidth(text) / 2, ly, n(line, "color"), "folder-warning-" + i);
      }
      check(pathLines > 0 && String.join(" ", body).equals(CONSEQUENCE), "warning omitted or shortened full availability consequence after path");
      check(source.startsWith((String)call(lines.get(1), "text")), "warning path does not immediately follow title");
      // Escape cancels; the second warning confirms through its real native action bounds.
      key(options, Keyboard.KEY_ESCAPE); check(!library.modal(), "Escape did not cancel folder warning");
      press(options, n(library, "sourceRemoveX") + 5, y + 7, 0);
      press(options, n(folders, "dialogLeft") + 6, n(folders, "buttonY") + 7, 0);
      check(!library.modal() && !MusicFolders.decode(setting(options, "audio.musicDirectories").value).contains(source), "Remove did not remove source preference");
      check(Arrays.equals(Files.readAllBytes(marker), bytes) && setting(options, "audio.playlists").value.equals(membership)
          && setting(options, "audio.favourites").value.equals(favorites), "Remove changed files or playlist membership");
      observations.add(Map.of("case", "folder-warning", "width", size[0], "pathLines", pathLines, "consequenceLines", body.size(), "markerPreserved", true));
    }
  }

  private static void volumeSteps(Minecraft mc, Release462Checks.State state) throws Exception {
    var options = open(state, SIZES[0]);
    List<Setting> settings = options.session().settings().stream().filter(VolumeControls::accepts).toList();
    check(!settings.isEmpty(), "actual ConfigSession has no volume controls");
    Set<String> ids = new HashSet<>(); int ambient = 0, categories = 0, sounds = 0, tracks = 0;
    for (Setting actual : settings) {
      ids.add(actual.id); boolean fraction = actual.id.startsWith("power_environment:config.MUSIC_CONFIG.volume");
      if (fraction) ambient++;
      if (actual.id.startsWith("audio.category.")) categories++;
      if (actual.id.startsWith("audio.sound.music:")) tracks++;
      else if (actual.id.startsWith("audio.sound.")) sounds++;
      double max = fraction ? 1 : 100, step = fraction ? .01 : 1;
      check(actual.min == 0 && actual.max == max && Math.abs(actual.step - step) < 1e-10,
          actual.id + " does not expose a 1% range and step");
      var original = actual.value.deepCopy();
      // Clone the actual discovered metadata. No changed(), preview or backend calls run in this loop.
      var isolated = new Setting(actual.id, actual.backend, actual.page, actual.group, actual.label, actual.description,
          actual.kind, actual.value, actual.defaultValue, actual.min, actual.max, actual.step, actual.choices, actual.restart);
      for (int percent = 0; percent <= 100; percent++) {
        double expected = percent * max / 100d;
        isolated.slide(percent / 100d); actual.validate(isolated.value); value(isolated, expected, "slide " + percent);
        check(isolated.display().equals(percent + "%") && isolated.editText().equals(Integer.toString(percent)), actual.id + " displayed wrong percent at " + percent);
        isolated.cycle(1); actual.validate(isolated.value); value(isolated, (percent == 100 ? 0 : percent + 1) * max / 100d, "cycle + " + percent);
        isolated.slide(percent / 100d); isolated.cycle(-1); actual.validate(isolated.value);
        value(isolated, (percent == 0 ? 100 : percent - 1) * max / 100d, "cycle - " + percent);
        isolated.parse(Integer.toString(percent)); actual.validate(isolated.value); value(isolated, expected, "exact " + percent);
      }
      check(actual.value.equals(original), "volume matrix changed actual session value for " + actual.id);
      observations.add(Map.of("case", "volume-101-positions", "id", actual.id, "rawMax", max, "rawStep", step, "positions", 101));
    }
    check(ids.containsAll(Set.of("native.music", "native.sound", "audio.master")) && ambient == 4
        && categories > 0 && sounds > 0 && tracks > 0, "actual volume matrix omitted native/master/category/sound/track or one of four ambient controls");
  }

  private static void value(Setting setting, double expected, String action) {
    check(Math.abs(setting.value.getAsDouble() - expected) < 1e-8, setting.id + " " + action + " gave " + setting.value + ", expected " + expected);
  }

  private static void exactVolume(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) {
      var options = open(state, size); var library = library(options); expand(library); int y = groupY(library, group);
      press(options, n(library, "groupVolumeX") + 3, y + 7, 1); exact(options, "37");
      check(setting(options, "audio.libraryGroupVolumes").value.getAsJsonObject().get(group).getAsInt() == 37, "group exact entry did not apply 37%");
      y = trackY(library, FIRST); var row = (MusicRowLayout)call(library, "rowLayout");
      press(options, row.volumeX() + 3, y + row.controlsY() + 6, 1); exact(options, "37");
      check(setting(options, "audio.sound." + FIRST).value.getAsInt() == 37, "track exact entry did not apply 37%");
      check(setting(options, "audio.playlists").value.toString().contains(SECOND), "volume exact entry changed playlist membership");
    }
  }

  private static void exact(PowerOptionsScreen options, String text) throws Exception {
    Object editor = get(options, "valueEditor"); check((boolean)call(editor, "active"), "native exact-entry editor did not open");
    ((TextInput)get(editor, "input")).setText(text); key(options, Keyboard.KEY_RETURN);
    check(!(boolean)call(editor, "active"), "valid percent exact entry stayed open");
  }
  private static Setting setting(PowerOptionsScreen options, String id) { return options.session().settings().stream().filter(s -> s.id.equals(id)).findFirst().orElseThrow(); }
  private static void tab(PowerOptionsScreen options, MusicLibraryScreen library, String id) throws Exception {
    var filters = (MusicFilters.Layout)call(library, "filters");
    var visible = filters.tabs().stream().filter(value -> value.id().equals(id)).findFirst();
    if (visible.isPresent()) {
      var tab = visible.get(); press(options, tab.x() + Math.min(6, tab.width() - 1), tab.y() + 7, 0);
    } else {
      int index = 0; while (index < filters.hiddenTabs().size() && !filters.hiddenTabs().get(index).id().equals(id)) index++;
      check(index < filters.hiddenTabs().size(), "Music tab is unavailable: " + id);
      press(options, filters.reloadX() + 5, filters.actionsY() + 7, 0); key(options, Keyboard.KEY_HOME);
      for (int item = 0; item < index; item++) key(options, Keyboard.KEY_DOWN);
      key(options, Keyboard.KEY_RETURN);
    }
  }
  private static void catalogEditor(PowerOptionsScreen options, MusicLibraryScreen library, String id) throws Exception {
    if (!library.state().folder().equals("playlists")) tab(options, library, "playlists");
    if (library.playlistEditor()) tab(options, library, "playlists");
    catalogRow(options, library, id);
  }
  private static void catalogRow(PowerOptionsScreen options, MusicLibraryScreen library, String id) throws Exception {
    Object panel = panel(library); List<MusicPlaylist> values = (List<MusicPlaylist>)call(panel, "filteredPresets");
    int index = 0; while (index < values.size() && !values.get(index).id().equals(id)) index++;
    check(index < values.size(), "catalog fixture playlist missing: " + id);
    int rowHeight = n(panel, "rowHeight"), viewport = n(panel, "bottom") - n(panel, "rowTop");
    int scroll = integer(panel, "scroll");
    if (index * rowHeight < scroll || (index + 1) * rowHeight > scroll + viewport) set(panel, "scroll", index * rowHeight);
    int y = n(panel, "rowTop") + index * rowHeight - integer(panel, "scroll") + 7;
    int x = n(call(panel, "catalogLayout"), "nameX") + 5;
    press(options, x, y, 0); release(options, x, y, 0); call(library, "syncPanel");
    check(library.playlistEditor() && draft(library).id().equals(id), "native catalog click did not enter playlist " + id);
  }
  private static void groupView(PowerOptionsScreen options, MusicLibraryScreen library) throws Exception {
    if (!(boolean)call(panel(library), "grouped")) {
      Object header = call(panel(library), "header");
      press(options, n(header, "viewX") + 5, n(panel(library), "actionY") + 7, 0);
    }
  }
  private static int trackY(MusicLibraryScreen library, String id) throws Exception {
    for (Object row : (List<?>)get(library, "rows")) if (id.equals(call(row, "id"))) {
      call(library, "scroll", new Class<?>[]{int.class}, Math.max(0, n(row, "y") - 20));
      return n(library, "listTop") + n(row, "y") - n(library, "scroll");
    }
    throw new AssertionError("native track row missing " + id);
  }
  private static void dirtyChoice(PowerOptionsScreen options, MusicLibraryScreen library, int choice) throws Exception {
    Object panel = panel(library); press(options, n(panel, "dialogButtonX", new Class<?>[]{int.class}, choice) + 6, n(panel, "dialogButtonY") + 7, 0);
  }
  private static void assertTooltip(Minecraft mc, Screen screen, int[] size, String destination, int mx, int my, String label) throws Exception {
    String text = "Back to " + destination;
    BufferedImage actual = shot(mc, screen, "check-tooltip-" + label, size, mx, my, false);
    Object bounds = call(Class.forName("local.luke.power.ui.TooltipLayout"), "at",
        new Class<?>[]{int.class, int.class, int.class, int.class, int.class, int.class},
        size[0], size[1], mc.textRenderer.getWidth(text), 1, mx, my);
    textMask(mc, actual, size, text, n(bounds, "x") + 4, n(bounds, "y") + 4, 0xeeeeee, "Back tooltip " + destination);
  }

  private static void adding(PowerOptionsScreen options, MusicLibraryScreen library) throws Exception {
    Object panel = panel(library); press(options, n(panel, "addX") + 6, n(panel, "actionY") + 7, 0);
    key(options, Keyboard.KEY_HOME); key(options, Keyboard.KEY_RETURN); call(library, "syncPanel");
    check((boolean)call(panel, "adding"), "Add songs From Library did not open");
  }
  private static void localBack(PowerOptionsScreen options, MusicLibraryScreen library) throws Exception {
    Object header = call(panel(library), "header"); press(options, n(header, "backX") + 6, n(panel(library), "top") + 7, 0);
  }
  private static void topBack(PowerOptionsScreen options) throws Exception { press(options, n(options, "left") + 6, 35, 0); }
  private static MusicLibraryScreen library(PowerOptionsScreen options) throws Exception { return (MusicLibraryScreen)get(options, "library"); }
  private static Object panel(MusicLibraryScreen library) throws Exception { return get(library, "presets"); }
  private static MusicPresetDraft draft(MusicLibraryScreen library) throws Exception { return (MusicPresetDraft)call(panel(library), "draft"); }
  private static void query(MusicLibraryScreen library, String text) throws Exception { ((TextInput)get(library, "search")).setText(text); call(library, "rebuild"); }
  private static void expand(MusicLibraryScreen library) throws Exception { ((Set<?>)get(library, "collapsed")).clear(); call(library, "rebuildRows"); }
  private static int groupY(MusicLibraryScreen library, String id) throws Exception {
    for (Object row : (List<?>)get(library, "rows")) if (call(row, "id") == null && id.equals(call(row, "group"))) {
      call(library, "scroll", new Class<?>[]{int.class}, Math.max(0, n(row, "y")));
      return n(library, "listTop") + n(row, "y") - n(library, "scroll");
    }
    throw new AssertionError("synthetic Library group missing");
  }
  private static void press(Screen screen, int x, int y, int button) { ((ScreenInput)(Object)screen).power$click(x, y, button); }
  private static void release(Screen screen, int x, int y, int button) { ((ScreenInput)(Object)screen).power$release(x, y, button); }
  private static void key(Screen screen, int key) { ((ScreenInput)(Object)screen).power$key('\0', key); }
  private static BufferedImage shot(Minecraft mc, Screen screen, String label, int[] size, int x, int y, boolean actual) throws Exception {
    String file = label + "-" + size[0] + ".png";
    Path path = (actual ? REPORT : REPORT.resolve("probes")).resolve(file);
    SettingsSnapshot.render(mc, screen, size[0], size[1], path, x, y);
    if (actual) captures.add(Map.of("file", file, "view", label, "screen", screen.getClass().getName(),
        "width", size[0], "height", size[1], "pointerX", x, "pointerY", y, "source", "native application screen"));
    return ImageIO.read(path.toFile());
  }
  private static int pixel(BufferedImage image, int x, int y) { return image.getRGB(x * 2, y * 2) & 0xffffff; }
  private static int countColor(BufferedImage image, int x, int y, int width, int height, int color) {
    int count = 0; for (int py = y; py < y + height; py++) for (int px = x; px < x + width; px++) if (pixel(image, px, py) == color) count++; return count;
  }
  private static void textMask(Minecraft mc, BufferedImage actual, int[] size, String text, int x, int y, int color, String label) throws Exception {
    var probe = new TextProbe(text, x, y, color); probe.init(mc, size[0], size[1]);
    BufferedImage reference = shot(mc, probe, "reference-" + label.replaceAll("[^A-Za-z0-9]+", "-"), size, -1, -1, false);
    int ink = 0;
    for (int py = y; py < y + 8; py++) for (int px = x; px < x + mc.textRenderer.getWidth(text); px++) {
      boolean expected = pixel(reference, px, py) == color; if (expected) ink++;
      check((pixel(actual, px, py) == color) == expected, label + " native font mask differs");
    }
    check(ink > 0, label + " reference contains no native font pixels");
  }
  private static final class TextProbe extends UiScreen {
    private final String label; private final int x, y, color;
    TextProbe(String label, int x, int y, int color) { this.label = label; this.x = x; this.y = y; this.color = color; }
    @Override public void render(int mx, int my, float delta) { fill(0, 0, width, height, 0xff000000); text(label, x, y, color); }
  }
  private static int integer(Object owner, String name) throws Exception { return ((Number)get(owner, name)).intValue(); }
  private static int n(Object owner, String name) throws Exception { return ((Number)call(owner, name)).intValue(); }
  private static int n(Object owner, String name, Class<?>[] types, Object... args) throws Exception { return ((Number)call(owner, name, types, args)).intValue(); }
  private static Field field(Class<?> type, String name) throws Exception {
    for (; type != null; type = type.getSuperclass()) try { var field = type.getDeclaredField(name); field.setAccessible(true); return field; } catch (NoSuchFieldException ignored) {}
    throw new NoSuchFieldException(name);
  }
  private static Object get(Object owner, String name) throws Exception { return field(owner instanceof Class<?> type ? type : owner.getClass(), name).get(owner instanceof Class<?> ? null : owner); }
  private static void set(Object owner, String name, Object value) throws Exception { field(owner instanceof Class<?> type ? type : owner.getClass(), name).set(owner instanceof Class<?> ? null : owner, value); }
  private static Object call(Object owner, String name) throws Exception { return call(owner, name, new Class<?>[0]); }
  private static Object call(Object owner, String name, Class<?>[] types, Object... args) throws Exception {
    for (Class<?> type = owner instanceof Class<?> ownerClass ? ownerClass : owner.getClass(); type != null; type = type.getSuperclass()) try {
      var method = type.getDeclaredMethod(name, types); method.setAccessible(true); return method.invoke(owner instanceof Class<?> ? null : owner, args);
    } catch (NoSuchMethodException ignored) {}
    throw new NoSuchMethodException(name);
  }
  private static final class SourceState implements AutoCloseable {
    private final Map<String, Object> values = new LinkedHashMap<>();
    private final Path root;
    SourceState() throws Exception {
      for (String name : List.of("sourceLibrary", "rotationConfig", "enabledTracks")) values.put(name, get(AudioController.class, name));
      root = Files.createTempDirectory(Path.of(".").toAbsolutePath(), "ui466-");
      Path folder = root.resolve("Synthetic building music folder with a long descriptive name")
          .resolve("Another long nested folder name for compact warning wrapping")
          .resolve("Final source folder with deliberately long identity");
      Files.createDirectories(folder); Files.writeString(folder.resolve("fixture-marker.txt"), "Owned disposable fixture; this file must survive Remove folder.\n");
      source = folder.toString(); group = MusicLibrarySources.id(source);
    }
    public void close() throws Exception {
      try { for (var entry : values.entrySet()) set(AudioController.class, entry.getKey(), entry.getValue()); }
      finally {
        try (var files = Files.walk(root)) {
          for (Path path : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
        }
      }
    }
  }
}
