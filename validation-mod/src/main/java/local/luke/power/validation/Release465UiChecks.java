package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.awt.image.BufferedImage;
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
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

/** Native Music navigation and shared control captures. No imported files or playback. */
final class Release465UiChecks {
  private static final Path REPORT = Path.of("power-beta-data/reports/release-465/ui");
  private static final int[][] SIZES = {{854, 480}, {320, 240}};
  private static final String CUSTOM = "ui465-building", NAME = "Building sessions";
  private static final String FIRST = "music:calm1.ogg", SECOND = "music:calm2.ogg", THIRD = "music:creative1.ogg";
  private static final String SOURCE = "music/ui465-synthetic-folder", GROUP = MusicLibrarySources.id(SOURCE);
  private static final int YELLOW = 0xffffa0, GOLD = 0xffcc33;
  private static final List<Map<String, Object>> captures = new ArrayList<>(), observations = new ArrayList<>();

  private Release465UiChecks() {}

  static void run(Minecraft mc, String action) throws Exception {
    check(Set.of("capture", "check").contains(action), "use release-465-ui capture|check");
    SilentAudioChecks.ready(mc);
    check(mc.world != null && mc.player != null && !mc.world.isRemote, "open a disposable singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable client");
    check(Display.isActive(), "focus the disposable client");
    check(AudioController.currentTrackId().isEmpty(), "start with idle disposable music");
    Files.createDirectories(REPORT.resolve("probes")); captures.clear(); observations.clear(); failures = 0;
    try (var sources = new SourceState(); var state = new Release462Checks.State(mc)) {
      // Capture every changed view before acceptance checks can fail.
      capture(mc, state);
      if (action.equals("check")) {
        test("RELEASE465 UI local Back restores Music views, catalog search and scroll", () -> navigation(mc, state));
        test("RELEASE465 UI Add browser Back retains its isolated draft and view", () -> additions(mc, state));
        test("RELEASE465 UI clean and dirty Show song and queue visits restore their owning playlist", () -> songNavigation(mc, state));
        test("RELEASE465 UI leaving Add songs restores its nested view after Save or Discard", () -> nestedAdditions(mc, state));
        test("RELEASE465 UI newly saved playlists remain valid Back destinations", () -> createdPlaylists(mc, state));
        test("RELEASE465 UI dirty top Back keeps, discards or saves then exits Audio", () -> dirtyExits(mc, state));
        test("RELEASE465 UI protected titles, group widths, counts and outer action bounds", () -> geometry(mc, state));
        test("RELEASE465 UI native Back tooltips name actual destinations", () -> tooltips(mc, state));
        test("RELEASE465 UI popup labels, selected markers, Defaults and selector chevrons", () -> dropdowns(mc, state));
      }
    } finally {
      Files.writeString(REPORT.resolve("captures.json"), Catalog.JSON.toJson(captures));
      Files.writeString(REPORT.resolve(action + ".json"), Catalog.JSON.toJson(Map.of("action", action,
          "failures", failures, "observations", observations, "limits", List.of(
              "Folder ownership is synthetic and uses existing built-in tracks; this does not test imports or decoding.",
              "All evidence captures render actual application screens; probes are excluded from captures.json.",
              "Pointer input uses the native Screen input bridge; physical desktop focus changes remain manual."))));
    }
    log("RELEASE465 UI " + action + " failures=" + failures + " actualCaptures=" + captures.size());
    check(failures == 0, "release-465-ui failures=" + failures);
  }

  private static PowerOptionsScreen open(Release462Checks.State state, int[] size) throws Exception {
    if (state.options != null) { state.options.session().discard(); state.options.removed(); }
    var audio = new AudioSettings();
    audio.playlists.add(new MusicPlaylist(CUSTOM, NAME, List.of(FIRST, SECOND, THIRD)));
    for (int i = 0; i < 14; i++) audio.playlists.add(new MusicPlaylist("ui465-list-" + i,
        "Building playlist " + i, List.of(FIRST, SECOND)));
    audio.favourites.add(FIRST); audio.sounds.put(FIRST, 35);
    audio.musicDirectories.add(SOURCE); audio.libraryGroupVolumes.put(GROUP, 40);
    MusicPlaylist.load(audio, CUSTOM); MusicPlaylist.loadMenu(audio, MusicPlaylist.SAME_AS_WORLD);
    AudioConfig.preview(audio);
    set(AudioController.class, "sourceLibrary", new MusicLibrarySources.Snapshot(
        List.of(new MusicLibrarySources.Folder(GROUP, SOURCE, "Building music folder", "Ready", List.of(FIRST, SECOND), List.of())),
        Map.of(FIRST, GROUP, SECOND, GROUP), List.of()));
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
    for (int[] size : SIZES) for (String view : List.of("library-groups", "playlist-local-back", "playlist-add-browser",
        "beta-title", "favorites-title", "top-back-audio-tooltip", "local-back-tooltip", "dirty-top-back",
        "selector-button-hover", "selector-selected-hover", "selector-unselected-hover", "disabled-popup-hover",
        "default-popup-hover", "all-sounds-back-tooltip", "hud-f3-back-tooltip", "panorama-back-tooltip")) {
      var options = open(state, size); var library = library(options); Screen screen = options; int mx = -1, my = -1;
      switch (view) {
        case "library-groups" -> { expand(library); groupY(library, GROUP); }
        case "playlist-local-back", "playlist-add-browser", "local-back-tooltip", "dirty-top-back" -> {
          library.openPresets(); library.showPlaylist(CUSTOM);
          if (view.equals("playlist-add-browser")) adding(options, library);
          if (view.equals("local-back-tooltip")) { Object header = call(panel(library), "header"); mx = n(header, "backX") + 6; my = n(panel(library), "top") + 7; }
          if (view.equals("dirty-top-back")) { draft(library).include(SECOND, false); call(library, "syncPanel"); topBack(options); }
        }
        case "beta-title" -> library.showPlaylist(MusicPlaylist.BETA);
        case "favorites-title" -> library.showPlaylist(MusicPlaylist.FAVORITES);
        case "top-back-audio-tooltip" -> { library.showPlaylist(CUSTOM); mx = n(options, "left") + 6; my = 35; }
        case "selector-button-hover" -> { Object button = call(options, "playlistButton"); mx = n(button, "x") + 6; my = n(button, "y") + 7; }
        case "selector-selected-hover", "selector-unselected-hover" -> {
          selector(options); Object choices = get(options, "choices"); int index = integer(choices, "selected");
          if (view.equals("selector-unselected-hover")) index = index == 0 ? 1 : 0;
          revealChoice(choices, index); mx = integer(choices, "x") + 12; my = popupY(choices, index) + 7;
        }
        case "disabled-popup-hover" -> {
          disabledPopup(options, library); Object choices = get(options, "choices"); mx = integer(choices, "x") + 12; my = popupY(choices, 3) + 7;
        }
        case "default-popup-hover" -> {
          defaultPopup(options); Object choices = get(options, "choices"); int index = integer(choices, "defaultIndex");
          revealChoice(choices, index); mx = integer(choices, "x") + 12; my = popupY(choices, index) + 7;
        }
        case "all-sounds-back-tooltip" -> { call(options, "closeLibrary"); call(options, "openAllSounds"); mx = n(options, "left") + 6; my = 35; }
        case "hud-f3-back-tooltip" -> {
          call(options, "closeLibrary"); var hud = new HudEditorScreen(options, HudEditorSettings.ENTRY);
          mc.setScreen(hud); hud.init(mc, size[0], size[1]); hud.beginReorder(); screen = hud;
          Object back = footerBack(hud); mx = n(back, "x") + 5; my = size[1] - 21;
        }
        case "panorama-back-tooltip" -> {
          call(options, "closeLibrary"); set(options, "page", "General"); call(options, "layout");
          var panorama = new PanoramaScreen(options, setting(options, "power_controls:userinterface.panoramaConfig.panoramaFolder"));
          mc.setScreen(panorama); panorama.init(mc, size[0], size[1]); screen = panorama; mx = size[0] / 2 - 44; my = size[1] - 20;
        }
      }
      shot(mc, screen, view, size, mx, my, true);
    }
  }

  private static void navigation(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) {
      var options = open(state, size); var library = library(options); library.openPresets(); query(library, "Building");
      Object panel = panel(library); set(panel, "scroll", n(panel, "rowHeight") * 5);
      int before = integer(panel, "scroll"), y = n(panel, "rowTop") + 10;
      List<?> values = (List<?>)call(panel, "filteredPresets");
      String id = ((MusicPlaylist)values.get(before / n(panel, "rowHeight"))).id();
      int x = n(call(panel, "catalogLayout"), "nameX") + 5;
      press(options, x, y, 0); release(options, x, y, 0); call(library, "syncPanel");
      check(library.playlistEditor() && draft(library).id().equals(id), "native catalog row did not open the chosen playlist");
      localBack(options, library);
      check(!library.playlistEditor() && library.presetVisible() && library.query().equals("Building")
          && integer(panel, "scroll") == before, "local Back lost catalog query or scroll");
      library.showTracks(); query(library, "Minecraft"); expand(library);
      call(library, "selectFilter", new Class<?>[]{String.class}, "favorites");
      check(draft(library).favorites(), "Favorites tab did not open Favorites"); localBack(options, library);
      check(!library.playlistEditor() && library.query().equals("Minecraft")
          && library.state().folder().equals("library"), "Favorites local Back did not restore Library");
      library.showPlaylist(CUSTOM); topBack(options);
      check(!(boolean)call(options, "libraryVisible") && get(options, "page").equals("Audio")
          && !(boolean)get(options, "allSounds") && ((Deque<?>)get(library, "history")).isEmpty(), "top Back did not exit to Audio and clear Music history");
      for (Object view : (Deque<?>)get(options, "history")) check(!(boolean)call(view, "libraryOpen"), "global history retained Music");
      call(options, "openMusicEditor");
      check(library.state().folder().equals("library") && library.query().isEmpty()
          && ((Deque<?>)get(library, "history")).isEmpty(), "reopening Music retained an old destination");
      observations.add(Map.of("case", "music-navigation", "width", size[0], "catalogScrollRestored", before));
    }
  }

  private static void additions(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) {
      var options = open(state, size); var library = library(options); library.showPlaylist(CUSTOM);
      var owner = draft(library); owner.include(SECOND, false); call(library, "syncPanel"); query(library, "a");
      call(library, "scroll", new Class<?>[]{int.class}, 24);
      int scroll = integer(library, "trackScroll"); var saved = MusicPlaylist.find(AudioConfig.current(), CUSTOM).tracks();
      adding(options, library); query(library, "Sweden"); localBack(options, library);
      check(draft(library) == owner && !owner.included(SECOND) && library.playlistDirty(), "Add browser Back replaced or published the isolated draft");
      check(library.query().equals("a") && integer(library, "trackScroll") == scroll
          && !(boolean)call(panel(library), "adding"), "Add browser Back lost playlist search, scroll or view");
      check(MusicPlaylist.find(AudioConfig.current(), CUSTOM).tracks().equals(saved), "Add browser navigation saved membership");
    }
  }

  private static void songNavigation(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) for (boolean dirty : new boolean[]{false, true}) for (String route : List.of("song", "library", "queue")) {
      var options = open(state, size); var library = library(options); library.showPlaylist(CUSTOM);
      set(panel(library), "grouped", true); call(library, "syncPanel"); query(library, "a"); expand(library);
      call(library, "scroll", new Class<?>[]{int.class}, 24);
      var owner = draft(library); if (dirty) { owner.include(SECOND, false); call(library, "syncPanel"); }
      int scroll = integer(library, "trackScroll");
      var controller = new LinkedHashMap<String, Object>();
      for (String field : List.of("currentMusic", "menuMusic", "currentPlaylistId", "currentEntry", "paused")) controller.put(field, get(AudioController.class, field));
      try {
        // A descriptor supplies native status navigation without starting any sound source.
        set(AudioController.class, "currentMusic", FIRST); set(AudioController.class, "menuMusic", "");
        set(AudioController.class, "currentPlaylistId", CUSTOM); set(AudioController.class, "currentEntry", AudioController.resolveTrack(FIRST));
        set(AudioController.class, "paused", true);
        Runnable visit = () -> {
          try {
            if (route.equals("queue")) library.showQueue();
            else if (route.equals("library")) library.showTrack(FIRST);
            else if (size[0] == 854) {
              shot(mc, options, "check-show-song-source", size, -1, -1, false);
              Object status = get(options, "musicStatus"); press(options, integer(status, "labelX") + 4, integer(status, "top") + 6, 0);
            } else library.showActiveTrack(FIRST);
          } catch (Exception failure) { throw new RuntimeException(failure); }
        };
        visit.run();
        if (dirty) {
          check(library.modal() && draft(library) == owner, route + " navigation dropped its dirty draft before prompting");
          dirtyChoice(options, library, 2); check(draft(library) == owner && library.query().equals("a"), "Keep editing lost Show song source state");
          visit.run(); dirtyChoice(options, library, 1);
        }
        if (route.equals("song")) localBack(options, library); else library.back(() -> {});
        check(library.playlistEditor() && draft(library).id().equals(CUSTOM) && (boolean)call(panel(library), "grouped")
            && library.query().equals("a") && integer(library, "trackScroll") == scroll,
            route + " Back lost source playlist identity, grouping, query or scroll");
      } finally { for (var entry : controller.entrySet()) set(AudioController.class, entry.getKey(), entry.getValue()); }
    }
  }

  private static void nestedAdditions(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) for (int action : new int[]{0, 1}) {
      var options = open(state, size); var library = library(options); library.showPlaylist(CUSTOM);
      query(library, "a"); call(library, "scroll", new Class<?>[]{int.class}, 24); int scroll = integer(library, "trackScroll");
      adding(options, library); draft(library).include(SECOND, false); call(library, "syncPanel"); query(library, "Sweden");
      call(library, "selectFilter", new Class<?>[]{String.class}, "favorites");
      check(library.modal(), "leaving dirty Add songs did not prompt"); dirtyChoice(options, library, action); localBack(options, library);
      check((boolean)call(panel(library), "adding") && library.query().equals("Sweden"), "Back from Favorites lost the Add browser view");
      check(integer(library, "playlistScroll") == scroll, "restored Add browser lost its saved owning scroll");
      check(draft(library).included(SECOND) == (action == 1), "restored Add browser ignored saved/discarded membership");
      localBack(options, library);
      int maximum = Math.max(0, integer(library, "contentHeight") - n(library, "bottom") + n(library, "listTop"));
      check(!(boolean)call(panel(library), "adding") && library.query().equals("a") && integer(library, "trackScroll") == Math.min(scroll, maximum),
          "restored Add browser Back lost its owning playlist search or scroll");
    }
  }

  private static void dirtyChoice(PowerOptionsScreen options, MusicLibraryScreen library, int action) throws Exception {
    Object panel = panel(library); press(options, n(panel, "dialogButtonX", new Class<?>[]{int.class}, action) + 6, n(panel, "dialogButtonY") + 7, 0);
  }

  private static void createdPlaylists(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) for (int action : new int[]{0, 1}) {
      var options = open(state, size); var library = library(options); library.openPresets(); call(library, "newPlaylist");
      ((TextInput)get(panel(library), "name")).setText("New building playlist"); String id = draft(library).id();
      call(library, "selectFilter", new Class<?>[]{String.class}, "favorites");
      check(library.modal(), "leaving new playlist did not prompt"); dirtyChoice(options, library, action);
      check(library.backDestination().equals(action == 0 ? "New building playlist" : "Playlists"), "new playlist Back tooltip names an unavailable destination");
      localBack(options, library);
      check(action == 0 ? library.playlistEditor() && draft(library).id().equals(id)
          : !library.playlistEditor() && library.state().folder().equals("playlists"), "Back failed to restore saved new playlist or discarded catalog");
    }
  }

  private static void dirtyExits(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) for (int action : new int[]{2, 1, 0}) {
      var options = open(state, size); var library = library(options); library.showPlaylist(CUSTOM);
      var owner = draft(library); owner.include(SECOND, false); call(library, "syncPanel");
      var saved = MusicPlaylist.find(AudioConfig.current(), CUSTOM).tracks(); topBack(options);
      check(library.modal() && library.playlistDirty(), "dirty top Back bypassed its prompt");
      Object panel = panel(library);
      press(options, n(panel, "dialogButtonX", new Class<?>[]{int.class}, action) + 6, n(panel, "dialogButtonY") + 7, 0);
      if (action == 2) {
        check((boolean)call(options, "libraryVisible") && draft(library) == owner && library.playlistDirty()
            && !library.modal(), "Keep editing replaced or closed the draft");
        check(MusicPlaylist.find(AudioConfig.current(), CUSTOM).tracks().equals(saved), "Keep editing saved membership");
      } else {
        check(!(boolean)call(options, "libraryVisible") && get(options, "page").equals("Audio"), "confirmed top Back did not leave Music");
        var actual = MusicPlaylist.find(AudioConfig.current(), CUSTOM).tracks();
        check(action == 0 ? !actual.contains(SECOND) : actual.equals(saved), "Save/Discard produced wrong saved membership");
        check(((Deque<?>)get(library, "history")).isEmpty(), "confirmed exit retained Music history");
      }
    }
  }

  private static void geometry(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) {
      var options = open(state, size); var library = library(options); expand(library); int gy = groupY(library, GROUP);
      var row = (MusicRowLayout)call(library, "rowLayout"); int gx = n(library, "groupVolumeX"), gw = n(library, "groupVolumeWidth");
      check(gx + gw == row.volumeX() + row.volumeWidth() && row.volumeX() - gx == 16 && gw - row.volumeWidth() == 16,
          "Library group and child sliders lost their shared right edge or 16px width difference");
      check(n(library, "sourceRemoveX") == row.playX() && n(library, "sourceRemoveWidth") == row.buttonWidth()
          && row.playX() + row.buttonWidth() == n(library, "right"), "folder X outer bounds differ from child Play");
      if (size[0] == 854) check(gw == 150 && row.volumeWidth() == 134, "desktop slider widths differ from Game sounds");
      check(n(library, "countRight", new Class<?>[]{String.class}, GROUP) < gx, "track count is not before the group slider");
      BufferedImage actual = shot(mc, options, "check-library-groups", size, -1, -1, false);
      String count = "2 tracks"; int countX = n(library, "countRight", new Class<?>[]{String.class}, GROUP) - mc.textRenderer.getWidth(count);
      textMask(mc, actual, size, count, countX, gy + 6, 0xaaaaaa, "Library track count");
      press(options, gx, gy + 8, 1);
      check((boolean)call(get(options, "valueEditor"), "editing", new Class<?>[]{Object.class}, "musicGroup/" + GROUP), "group exact entry missed its drawn left edge");
      key(options, Keyboard.KEY_ESCAPE);
      for (String id : List.of(MusicPlaylist.BETA, MusicPlaylist.FAVORITES)) {
        library.showPlaylist(id); Object panel = panel(library), header = call(panel, "header");
        check(n(header, "backX") + n(header, "backWidth") + 4 == n(header, "nameX"), "Back is not immediately before the name");
        check(n(header, "nameX") + n(header, "nameWidth") <= n(library, "right")
            && n(header, "viewX") + n(header, "viewWidth") <= n(library, "right"), "compact editor controls escape the content bounds");
        check(n(library, "bottom") - n(library, "listTop") >= 24, "compact editor has no complete song row");
        actual = shot(mc, options, "check-title-" + id, size, -1, -1, false);
        int ix = n(header, "nameX") + 2, iy = n(panel, "top");
        var reference = new IconProbe(id.equals(MusicPlaylist.FAVORITES), ix, iy); reference.init(mc, size[0], size[1]);
        BufferedImage expected = shot(mc, reference, "reference-icon-" + id, size, -1, -1, false);
        int ink = 0;
        for (int y = iy; y < iy + 18; y++) for (int x = ix; x < ix + 16; x++) {
          int color = pixel(expected, x, y);
          if (color != 0) { ink++; check(pixel(actual, x, y) == color, "protected title icon differs from shared native icon"); }
        }
        check(ink > 0 && (!id.equals(MusicPlaylist.FAVORITES) || countColor(actual, ix, iy, 16, 18, GOLD) > 0), "protected title icon is absent");
      }
    }
  }

  private static void tooltips(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) {
      var options = open(state, size); var library = library(options); library.openPresets(); library.showPlaylist(CUSTOM);
      Object panel = panel(library), header = call(panel, "header");
      assertTooltip(mc, options, size, "Playlists", n(header, "backX") + 6, n(panel, "top") + 7, "local-back");
      assertTooltip(mc, options, size, "Audio settings", n(options, "left") + 6, 35, "top-back");
      adding(options, library);
      assertTooltip(mc, options, size, NAME, n(panel, "addX") + 6, n(panel, "actionY") + 7, "add-browser-back");
      ((TextInput)get(panel, "name")).setText(" Evening ");
      check(library.backDestination().equals("Evening"), "Add browser Back ignored the current draft name");
      header = call(panel, "header");
      assertTooltip(mc, options, size, "Evening", n(header, "backX") + 6, n(panel, "top") + 7, "renamed-local-back");
      assertTooltip(mc, options, size, "Evening", n(panel, "addX") + 6, n(panel, "actionY") + 7, "renamed-add-back");
      ((TextInput)get(panel, "name")).setText("");
      check(library.backDestination().equals(NAME), "empty draft name lost its Back fallback");
      ((TextInput)get(panel, "name")).setText(NAME);
      localBack(options, library); topBack(options); call(options, "openAllSounds");
      assertTooltip(mc, options, size, "Audio settings", n(options, "left") + 6, 35, "all-sounds-back");
      options = open(state, size); library = library(options); library.openPresets(); call(library, "newPlaylist");
      panel = panel(library); ((TextInput)get(panel, "name")).setText("Evening"); adding(options, library);
      check(library.backDestination().equals("Evening"), "new playlist Add browser Back used its initial name");
      assertTooltip(mc, options, size, "Evening", n(panel, "addX") + 6, n(panel, "actionY") + 7, "new-playlist-add-back");
    }
  }

  private static void dropdowns(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) {
      var options = open(state, size); var library = library(options); Object button = call(options, "playlistButton");
      int bx = n(button, "x"), by = n(button, "y"), bw = n(button, "width");
      BufferedImage hovered = shot(mc, options, "check-selector-hover", size, bx + 5, by + 7, false);
      selectorButton(mc, options, hovered, size, bx, by, bw, false);
      selector(options); Object choices = get(options, "choices"); int selected = integer(choices, "selected");
      for (int index : new int[]{selected, selected == 0 ? 1 : 0}) {
        revealChoice(choices, index); int x = integer(choices, "x"), y = popupY(choices, index);
        var actual = shot(mc, options, "check-choice-hover-" + index, size, x + 12, y + 7, false);
        choiceLabel(mc, options, choices, actual, size, index, YELLOW);
        check(pixel(actual, x + 1, y + 1) == (PopupStyle.HOVER & 0xffffff), "popup hover background changed");
        if (index == selected) check(pixel(actual, x + 2, y + 4) == (PopupStyle.ACTIVE & 0xffffff), "hover replaced the blue active marker");
        selectorButton(mc, options, actual, size, bx, by, bw, true);
      }
      key(options, Keyboard.KEY_ESCAPE); defaultPopup(options); choices = get(options, "choices");
      int index = integer(choices, "defaultIndex"); revealChoice(choices, index);
      int x = integer(choices, "x"), y = popupY(choices, index), rw = n(choices, "rowWidth");
      var actual = shot(mc, options, "check-default-hover", size, x + 12, y + 7, false);
      choiceLabel(mc, options, choices, actual, size, index, YELLOW);
      textMask(mc, actual, size, "Default", x + rw - mc.textRenderer.getWidth("Default") - PopupStyle.PADDING,
          y + (integer(choices, "rowHeight") - 8) / 2, 0x888888, "Default marker remains grey");
      key(options, Keyboard.KEY_ESCAPE); disabledPopup(options, library); choices = get(options, "choices");
      x = integer(choices, "x"); y = popupY(choices, 3);
      actual = shot(mc, options, "check-disabled-hover", size, x + 12, y + 7, false);
      choiceLabel(mc, options, choices, actual, size, 3, PopupStyle.DISABLED_TEXT);
      check(countColor(actual, x, y, n(choices, "rowWidth"), integer(choices, "rowHeight"), YELLOW) == 0, "disabled popup row turns yellow");
      press(options, x + 12, y + 7, 0); check((boolean)call(choices, "active") && !library.modal(), "disabled Delete accepted input");
      key(options, Keyboard.KEY_ESCAPE);
    }
  }

  private static void selectorButton(Minecraft mc, PowerOptionsScreen options, BufferedImage actual, int[] size,
      int x, int y, int width, boolean open) throws Exception {
    String fitted = (String)call(options, "fit", new Class<?>[]{String.class, int.class}, NAME, width - 38);
    if (mc.textRenderer.getWidth(fitted) <= width - 38)
      textMask(mc, actual, size, fitted, x + width / 2 - mc.textRenderer.getWidth(fitted) / 2, y + 5, YELLOW, "selector label hover/open");
    int cx = x + width - 11, cy = y + 7;
    for (int i = 0; i < 3; i++) {
      check(pixel(actual, cx + i, cy + i) == YELLOW, "selector chevron does not share yellow hover/open label");
      check(pixel(actual, open ? cx + 4 - i : cx + i, open ? cy + i : cy + 4 - i) == YELLOW, "selector chevron orientation or color changed");
    }
  }

  private static void choiceLabel(Minecraft mc, PowerOptionsScreen options, Object choices, BufferedImage actual,
      int[] size, int index, int color) throws Exception {
    String label = ((List<String>)get(choices, "labels")).get(index);
    int reserve = integer(choices, "defaultIndex") == index ? mc.textRenderer.getWidth("Default") + 3 : 0;
    String fit = (String)call(options, "fit", new Class<?>[]{String.class, int.class}, label, n(choices, "rowWidth") - 18 - reserve);
    textMask(mc, actual, size, fit, integer(choices, "x") + 9,
        popupY(choices, index) + (integer(choices, "rowHeight") - 8) / 2, color, "native popup label");
  }

  private static void defaultPopup(PowerOptionsScreen options) throws Exception {
    Setting setting = options.session().settings().stream().filter(s -> s.kind == Setting.Kind.CHOICE && s.choices.size() >= 2).findFirst().orElseThrow();
    call(get(options, "choices"), "open", new Class<?>[]{Setting.class, int.class, int.class, int.class, int.class, int.class, Runnable.class},
        setting, n(options, "left"), 55, Math.min(220, n(options, "span")), options.width, options.height, (Runnable)() -> {});
  }

  private static void disabledPopup(PowerOptionsScreen options, MusicLibraryScreen library) throws Exception {
    library.openPresets();
    call(panel(library), "context", new Class<?>[]{MusicPlaylist.class, int.class, int.class},
        MusicPlaylist.find(AudioConfig.current(), MusicPlaylist.BETA), n(options, "left") + 8, 90);
  }

  private static void assertTooltip(Minecraft mc, Screen screen, int[] size, String destination, int mx, int my, String label) throws Exception {
    String text = "Back to " + destination;
    BufferedImage actual = shot(mc, screen, "check-tooltip-" + label, size, mx, my, false);
    Object bounds = call(Class.forName("local.luke.power.ui.TooltipLayout"), "at",
        new Class<?>[]{int.class, int.class, int.class, int.class, int.class, int.class},
        size[0], size[1], mc.textRenderer.getWidth(text), 1, mx, my);
    textMask(mc, actual, size, text, n(bounds, "x") + 4, n(bounds, "y") + 4, 0xeeeeee, "Back tooltip " + destination);
  }

  private static Object footerBack(Screen screen) throws Exception {
    for (Object button : (List<?>)call(screen, "footer")) if (call(button, "label").equals("Back")) return button;
    throw new AssertionError("HUD F3 Back footer missing");
  }
  private static Setting setting(PowerOptionsScreen options, String id) { return options.session().settings().stream().filter(s -> s.id.equals(id)).findFirst().orElseThrow(); }
  private static void selector(PowerOptionsScreen options) throws Exception { Object button = call(options, "playlistButton"); press(options, n(button, "x") + 5, n(button, "y") + 7, 0); }
  private static void revealChoice(Object choices, int index) throws Exception { set(choices, "focus", index); call(choices, "reveal"); }
  private static int popupY(Object choices, int index) throws Exception { return integer(choices, "y") + n(get(choices, "layout"), "top", new Class<?>[]{int.class}, index) - integer(choices, "scroll"); }
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
  private static final class IconProbe extends UiScreen {
    private final boolean star; private final int x, y;
    private final net.minecraft.client.render.item.ItemRenderer renderer = new net.minecraft.client.render.item.ItemRenderer();
    IconProbe(boolean star, int x, int y) { this.star = star; this.x = x; this.y = y; }
    @Override public void render(int mx, int my, float delta) {
      fill(0, 0, width, height, 0xff000000);
      if (star) icon("star", x, y, 16, 18, true, true);
      else GuiItems.draw(renderer, minecraft, new net.minecraft.item.ItemStack(net.minecraft.block.Block.GRASS.id, 1, 0), x, y + 1, false);
    }
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
    SourceState() throws Exception { for (String name : List.of("sourceLibrary", "rotationConfig", "enabledTracks")) values.put(name, get(AudioController.class, name)); }
    public void close() throws Exception { for (var entry : values.entrySet()) set(AudioController.class, entry.getKey(), entry.getValue()); }
  }
}
