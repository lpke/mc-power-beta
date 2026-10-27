package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.mixin.SoundManagerAccessor;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

/** Native music acceptance and independent screenshots in a muted disposable world. */
final class Release464MusicUiChecks {
  private static final Path REPORT = Path.of("power-beta-data/reports/release-464/music-ui");
  private static final int[] LARGE = {854, 480}, SMALL = {320, 240};
  private static final String CUSTOM = "ui462-long", EMPTY = "ui462-empty";
  private static final String FIRST = "music:calm1.ogg", SECOND = "music:calm2.ogg", THIRD = "music:creative1.ogg";
  private Release464MusicUiChecks() {}

  static void run(Minecraft mc, String action) throws Exception {
    check(Set.of("capture", "check").contains(action), "use release-464-music-ui capture|check");
    SilentAudioChecks.ready(mc);
    check(mc.world != null && mc.player != null && !mc.world.isRemote, "open a disposable singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable client");
    check(Display.isActive(), "focus the disposable client for pointer checks");
    failures = 0;
    try (var playback = new PlaybackState(mc); var state = new Release462Checks.State(mc)) {
      state.playback = true;
      if (action.equals("capture")) capture(mc, state);
      else {
        test("RELEASE464 catalog body, radio, protected actions, duplicate and inline rename", () -> catalog(mc, state));
        test("RELEASE464 catalog drag persists, Escape cancels and changed source rejects", () -> reorder(mc, state));
        test("RELEASE464 ordered responsive tabs and ranked playlist and song aliases", () -> search(mc, state));
        test("RELEASE464 Library gains share playback, exact entry and Options rollback", () -> gains(mc, state));
        test("RELEASE464 normal paused gold, preview blue and preview pause preserve selection", () -> playback(mc, state));
        test("RELEASE464 selector secondary navigation and modal child popup hover", () -> navigation(mc, state));
        test("RELEASE464 real waiting status navigates playlist, outsider, queue and Audio", () -> waiting(mc, state));
      }
    }
    log("RELEASE464 MUSIC UI " + action + " failures=" + failures);
    check(failures == 0, "release-464-music-ui failures=" + failures);
  }

  private static void capture(Minecraft mc, Release462Checks.State state) throws Exception {
    Files.createDirectories(REPORT); var names = new ArrayList<String>();
    for (int[] size : List.of(LARGE, SMALL)) {
      for (String view : List.of("catalog", "tab-overflow", "catalog-context", "inline-rename", "duplicate", "read-only", "editor", "grouped", "library-global", "fuzzy", "fuzzy-cross-groups", "normal-paused", "preview-active", "preview-paused", "selector", "add-songs-popup", "from-playlist-popup", "waiting-gap")) {
        if (view.equals("waiting-gap") && size == SMALL || view.equals("tab-overflow") && size == LARGE) continue;
        AudioController.quiet();
        var options = state.open(size, CUSTOM, false); var library = library(options);
        int mx = -1, my = -1;
        switch (view) {
          case "catalog" -> library.openPresets();
          case "tab-overflow" -> {
            library.showTracks();
            click(options, n(library, "reloadX") + 5, n(library, "controlsTop") + 5, 0);
            Object popup = get(options, "choices"); mx = number(popup, "x") + 10; my = popupY(popup, 0) + 6;
          }
          case "catalog-context" -> { library.openPresets(); context(options, library, CUSTOM); }
          case "inline-rename" -> { library.openPresets(); rename(options, library, CUSTOM); }
          case "duplicate" -> {
            library.openPresets(); duplicate(options, library, CUSTOM);
            var copies = playlists(library); String id = copies.get(copies.size() - 1).id();
            context(options, library, id); choose(options, get(options, "choices"), 1);
          }
          case "read-only" -> library.showPlaylist(MusicPlaylist.BETA);
          case "editor" -> library.showPlaylist(CUSTOM);
          case "grouped" -> { library.showPlaylist(CUSTOM); grouped(library); }
          case "library-global" -> { library.showTracks(); expand(library); groupY(library, "Alpha"); }
          case "fuzzy" -> { library.showTracks(); query(library, "mnecraft"); expand(library); }
          case "fuzzy-cross-groups" -> {
            library.showTracks(); var moog = moogTracks(); forceFirstGroup(library, moog.get(1).group());
            query(library, moog.get(0).title()); expand(library);
          }
          case "normal-paused" -> { library.showTrack(FIRST); AudioController.playNow(FIRST); AudioController.pause(); }
          case "preview-active" -> { library.showTrack(FIRST); AudioController.previewSound(FIRST); }
          case "preview-paused" -> { library.showTrack(FIRST); AudioController.previewSound(FIRST); AudioController.pause(); }
          case "selector" -> { selector(options, 0); Object popup = get(options, "choices"); mx = number(popup, "x") + 12; my = popupY(popup, 3) + 5; }
          case "waiting-gap" -> { gap(mc, options, library, false); names.add("waiting-gap-854.png"); continue; }
          case "add-songs-popup" -> {
            library.showPlaylist(CUSTOM); Object panel = panel(library);
            click(options, n(panel, "addX") + 5, n(panel, "actionY") + 5, 0);
            Object popup = get(panel, "choices"); mx = number(popup, "x") + 10; my = popupY(popup, 0) + 6;
          }
          case "from-playlist-popup" -> {
            library.showPlaylist(CUSTOM); Object panel = panel(library);
            click(options, n(panel, "addX") + 5, n(panel, "actionY") + 5, 0);
            choose(options, get(panel, "choices"), 1);
            Object popup = get(panel, "choices"); mx = number(popup, "x") + 10; my = popupY(popup, 0) + 6;
          }
        }
        shot(mc, options, view, size, mx, my); names.add(view + "-" + size[0] + ".png");
      }
    }
    Files.writeString(REPORT.resolve("captures.json"), Catalog.JSON.toJson(names));
    log("RELEASE464 MUSIC UI captures=" + names.size());
  }

  private static void catalog(Minecraft mc, Release462Checks.State state) throws Exception {
    var options = state.open(LARGE, CUSTOM, false); var library = library(options); library.openPresets();
    Object panel = panel(library), layout = call(panel, "catalogLayout");
    int y = catalogY(library, CUSTOM), nameX = n(layout, "nameX");
    click(options, nameX + 5, y + 10, 0);
    check(!(boolean)call(panel, "editing"), "catalog editor opened before pointer release");
    release(options, nameX + 5, y + 10, -1);
    check((boolean)call(get(panel, "reorder"), "active"), "native motion release ended catalog drag");
    release(options, nameX + 5, y + 10, 0);
    check(draft(library).id().equals(CUSTOM), "catalog body did not open selected playlist");
    library.cancelPlaylist(); library.openPresets();
    y = catalogY(library, MusicPlaylist.BETA);
    click(options, n(layout, "useX") + 5, y + 10, 0);
    check(AudioConfig.current().playlist.equals(MusicPlaylist.BETA) && !library.playlistEditor(), "Use radio edited instead of selecting");
    shot(mc, options, "check-catalog-colors", LARGE, -1, -1);
    check(ink("check-catalog-colors", LARGE, nameX, y + 10, n(layout, "nameWidth"), 8, 0xffdd66), "selected playlist title lost gold");
    int customY = catalogY(library, CUSTOM);
    check(ink("check-catalog-colors", LARGE, nameX, customY + 10, n(layout, "nameWidth"), 8, 0xffffff), "ordinary playlist title is not white");
    check(ink("check-catalog-colors", LARGE, n(layout, "deleteX"), y + 5, 20, 18, 0x777777), "protected Delete cross lost disabled styling");
    shot(mc, options, "check-catalog-body-hover", LARGE, nameX + 5, customY + 10);
    shot(mc, options, "check-catalog-delete-hover", LARGE, n(layout, "deleteX") + 5, customY + 10);
    check(pixel("check-catalog-body-hover", LARGE, n(panel, "left") + 1, customY + 1)
        == pixel("check-catalog-delete-hover", LARGE, n(panel, "left") + 1, customY + 1), "catalog control hover differs from body hover");
    int before = playlists(library).size();
    click(options, n(layout, "deleteX") + 5, n(layout, "controlsY", new Class<?>[]{int.class}, y) + 6, 0);
    check(!(boolean)call(panel, "modal") && playlists(library).size() == before, "protected Delete was enabled");
    context(options, library, MusicPlaylist.BETA);
    Object popup = get(options, "choices");
    check(get(popup, "labels").equals(List.of("Play", "Rename", "Duplicate", "Delete")), "catalog context actions changed order");
    check(get(popup, "enabled").equals(List.of(true, false, true, false)), "protected context permissions differ");
    key(options, Keyboard.KEY_ESCAPE); duplicate(options, library, CUSTOM);
    check(playlists(library).size() == before + 1, "Duplicate did not create a saved playlist");
    MusicPlaylist copy = playlists(library).get(playlists(library).size() - 1);
    check(!copy.id().equals(CUSTOM) && copy.tracks().equals(MusicPlaylist.find(AudioConfig.current(), CUSTOM).tracks()), "Duplicate lost identity or membership");
    check(!library.playlistEditor(), "Duplicate opened an unsaved draft");
    rename(options, library, copy.id()); ((TextInput)get(panel, "rename")).setText("Renamed native copy");
    key(options, Keyboard.KEY_ESCAPE);
    check(MusicPlaylist.find(AudioConfig.current(), copy.id()).name().equals(copy.name()), "Escape committed inline rename");
    rename(options, library, copy.id()); ((TextInput)get(panel, "rename")).setText(" "); key(options, Keyboard.KEY_RETURN);
    check(library.modal() && !((String)call(panel, "error")).isEmpty(), "blank inline name passed validation");
    ((TextInput)get(panel, "rename")).setText("Favorites"); key(options, Keyboard.KEY_RETURN);
    check(library.modal() && MusicPlaylist.find(AudioConfig.current(), copy.id()).name().equals(copy.name()), "duplicate inline name passed validation");
    ((TextInput)get(panel, "rename")).setText("Renamed native copy"); key(options, Keyboard.KEY_RETURN);
    check(!library.modal() && MusicPlaylist.find(AudioConfig.current(), copy.id()).name().equals("Renamed native copy"), "Enter failed to save inline name");
    context(options, library, CUSTOM); choose(options, get(options, "choices"), 0);
    check(AudioConfig.current().playlist.equals(CUSTOM) && !library.playlistEditor(), "context Play did not select saved playlist");
    context(options, library, copy.id()); choose(options, get(options, "choices"), 3); key(options, Keyboard.KEY_ESCAPE);
    check(playlists(library).stream().anyMatch(p -> p.id().equals(copy.id())), "Escape deleted the playlist");
    context(options, library, copy.id()); choose(options, get(options, "choices"), 3);
    click(options, (LARGE[0] - Math.min(312, LARGE[0] - 20)) / 2 + 5, LARGE[1] / 2 + 9, 0);
    check(playlists(library).size() == before && AudioConfig.current().playlist.equals(CUSTOM), "confirmed Delete removed the wrong playlist or selection");
    AudioController.quiet(); options.session().discard();
  }

  private static void reorder(Minecraft mc, Release462Checks.State state) throws Exception {
    var options = state.open(LARGE, CUSTOM, false); var library = library(options); library.openPresets();
    Object panel = panel(library), layout = call(panel, "catalogLayout"); int x = n(layout, "nameX") + 5;
    List<String> original = ids(library); int top = n(panel, "rowTop"), height = n(panel, "rowHeight");
    click(options, x, top + 10, 0); release(options, x, top + 2 * height + 4, 0);
    List<String> reordered = ids(library);
    check(reordered.get(1).equals(original.get(0)) && !reordered.equals(original), "catalog drag failed to reorder protected entry");
    check(AudioConfig.current().playlistOrder.equals(reordered) && !library.playlistEditor(), "catalog drag was not persisted independently");
    library.openPresets(); check(ids(library).equals(reordered), "reopening catalog lost saved order");
    click(options, x, top + 10, 0); key(options, Keyboard.KEY_ESCAPE); release(options, x, top + 3 * height, 0);
    check(ids(library).equals(reordered) && !library.playlistEditor(), "Escape failed to cancel catalog gesture");
    click(options, x, top + 10, 0);
    Object edits = get(library, "edits");
    call(edits, "save", new Class<?>[]{MusicPlaylist.class}, new MusicPlaylist("native464-concurrent", "Concurrent playlist", List.of(FIRST)));
    List<String> changed = ids(library); release(options, x, top + 3 * height, 0);
    check(ids(library).equals(changed) && !((String)call(panel, "error")).isEmpty(), "changed catalog source accepted stale drag");
    options.session().discard();
  }

  private static void search(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : List.of(LARGE, SMALL)) {
      var options = state.open(size, CUSTOM, false); var library = library(options);
      for (String view : List.of("library", "playlists", "favorites", "active")) {
        call(library, "selectFilter", new Class<?>[]{String.class}, view);
        var tabs = (MusicFilters.Layout)call(library, "filters");
        check(tabs.tabs().stream().anyMatch(tab -> tab.id().equals(view)), "selected tab hidden at " + size[0]);
        check(tabs.tabs().get(0).id().equals("library"), "Library is not the first tab");
        if (size == LARGE) check(tabs.tabs().stream().map(MusicFilters.Tab::id).toList().equals(List.of("library", "playlists", "favorites", "active")), "music tab order changed");
        else check(tabs.overflow() && tabs.tabs().size() == 2 && tabs.hiddenTabs().size() == 2, "compact tabs lost overflow access");
      }
      library.openPresets(); List<String> order = ids(library);
      Object edits = get(library, "edits");
      call(edits, "save", new Class<?>[]{MusicPlaylist.class}, new MusicPlaylist("native464-prefix", "Rain music", List.of(FIRST)));
      call(edits, "save", new Class<?>[]{MusicPlaylist.class}, new MusicPlaylist("native464-exact", "Rain", List.of(SECOND)));
      call(edits, "save", new Class<?>[]{MusicPlaylist.class}, new MusicPlaylist("native464-fuzzy", "Raim", List.of(THIRD)));
      query(library, "rain"); var matched = playlists(library);
      check(matched.stream().map(MusicPlaylist::id).toList().equals(List.of("native464-exact", "native464-prefix", "native464-fuzzy")), "playlist exact/literal/fuzzy ranking differs");
      query(library, ""); check(ids(library).subList(0, order.size()).equals(order), "clearing search changed playlist order");
      library.showTracks(); expand(library); List<?> blank = List.copyOf((List<?>)get(library, "tracks"));
      for (String term : List.of("calm1", "Minecraft", "mnecraft")) {
        query(library, term); expand(library);
        @SuppressWarnings("unchecked") List<String> tracks = (List<String>)get(library, "tracks");
        check(tracks.contains(FIRST), "music filename/friendly/fuzzy alias missed " + term);
        int score = -1; for (String id : tracks) { int next = MusicSearch.trackScore(term, AudioController.musicLabel(id)); check(next >= score, "song rank decreased"); score = next; }
        int groupScore = -1;
        for (Object row : (List<?>)get(library, "rows")) if (call(row, "id") == null) {
          String group = (String)call(row, "group"); int best = tracks.stream().filter(id -> group.equals(uncheckedGroup(library, id)))
              .mapToInt(id -> MusicSearch.trackScore(term, AudioController.musicLabel(id))).min().orElse(MusicSearch.NONE);
          check(best >= groupScore, "group ordering hid a better song match"); groupScore = best;
        }
      }
      @SuppressWarnings("unchecked") List<String> normalGroups = (List<String>)get(library, "groupOrder");
      var aliases = moogTracks(); var exact = aliases.get(0); var weaker = aliases.get(1);
      check(!exact.group().equals(weaker.group()), "cross-group search fixture requires separate manifest groups");
      forceFirstGroup(library, weaker.group()); query(library, exact.title()); expand(library);
      @SuppressWarnings("unchecked") List<String> moog = (List<String>)get(library, "tracks");
      check(moog.indexOf(exact.id()) >= 0 && moog.indexOf(weaker.id()) >= 0 && moog.indexOf(exact.id()) < moog.indexOf(weaker.id()),
          "exact Moog City alias did not outrank Moog City 2");
      Object firstHeading = ((List<?>)get(library, "rows")).stream().findFirst().orElseThrow();
      check(call(firstHeading, "group").equals(exact.group()), "group order hid exact Moog City behind a weaker menu alias");
      set(library, "groupOrder", normalGroups); query(library, "");
      check(get(library, "tracks").equals(blank), "clearing song search changed normal order");
      options.session().discard();
    }
  }

  private static void gains(Minecraft mc, Release462Checks.State state) throws Exception {
    var options = state.open(LARGE, CUSTOM, false); var library = library(options); library.showTracks(); expand(library);
    int y = groupY(library, "Alpha"), x = n(library, "groupVolumeX"), width = n(library, "groupVolumeWidth");
    String savedPlaylists = Catalog.JSON.toJson(AudioConfig.current().playlists);
    MenuPreferences.current().autoApply = true;
    try {
      Path music = AudioConfig.path().getParent().getParent().resolve("power-beta/music.json");
      byte[] before = Files.exists(music) ? Files.readAllBytes(music) : null;
      int quarter = x + 4 + (int)Math.round((width - 8) * .25);
      click(options, quarter, y + 8, 0);
      check(Math.abs(AudioConfig.current().libraryGroupVolumes.getOrDefault("Alpha", 100) - 25) <= 1, "Library group drag missed its bounds");
      check((boolean)get(options, "autoSavePending"), "continuous group drag did not defer auto-save");
      check(Arrays.equals(before, Files.exists(music) ? Files.readAllBytes(music) : null), "group drag wrote before release");
      release(options, quarter, y + 8, -1); check(get(library, "draggingGroup") != null, "native motion release ended group drag");
      release(options, quarter, y + 8, 0); check(!(boolean)get(options, "autoSavePending"), "group release left deferred save pending");
    } finally { MenuPreferences.current().autoApply = false; }
    y = groupY(library, "Alpha"); click(options, x + 10, y + 8, 1);
    Object exact = get(options, "valueEditor"); check((boolean)call(exact, "active"), "Library group exact entry did not open");
    ((TextInput)get(exact, "input")).setText("31"); key(options, Keyboard.KEY_RETURN);
    check(AudioConfig.current().libraryGroupVolumes.getOrDefault("Alpha", 100) == 31, "group exact Enter missed");
    click(options, x + 10, groupY(library, "Alpha") + 8, 1); ((TextInput)get(exact, "input")).setText("99"); key(options, Keyboard.KEY_ESCAPE);
    check(AudioConfig.current().libraryGroupVolumes.getOrDefault("Alpha", 100) == 31, "group exact Escape committed");
    float expected = .31f * AudioConfig.current().sounds.getOrDefault(FIRST, 100) / 100f;
    for (String id : List.of(CUSTOM, MusicPlaylist.BETA, MusicPlaylist.FAVORITES)) {
      call(get(library, "edits"), "load", new Class<?>[]{String.class}, id);
      set(AudioController.class, "currentMusic", FIRST); set(AudioController.class, "paused", false);
      check(Math.abs(AudioController.backgroundVolume(1) - expected) < .00001, "global gain differs across saved playlist " + id);
    }
    check(Catalog.JSON.toJson(AudioConfig.current().playlists).equals(savedPlaylists), "Library gain changed saved playlist data");
    library.showPlaylist(CUSTOM); grouped(library);
    check(!(boolean)call(library, "groupHasVolume", new Class<?>[]{String.class}, "Alpha"), "grouped playlist still owns a group gain");
    check((int)call(library, "includedCount", new Class<?>[]{String.class}, "Alpha") == 2, "grouped playlist lost X/Y numerator");
    query(library, "Minecraft"); check((int)call(library, "includedCount", new Class<?>[]{String.class}, "Alpha") == 2, "search changed grouped playlist count");
    options.session().discard();
    check(Math.abs(AudioConfig.current().libraryGroupVolumes.getOrDefault("Alpha",100) - 25) <= 1, "Options Discard retained unsaved global gain");
  }

  private static void playback(Minecraft mc, Release462Checks.State state) throws Exception {
    var options = state.open(LARGE, CUSTOM, false); var library = library(options); library.showTrack(FIRST);
    AudioController.playNow(FIRST); AudioController.pause();
    shot(mc, options, "check-normal-paused", LARGE, -1, -1);
    check(trackInk("check-normal-paused", library, FIRST, 0xffdd66), "paused normal track lost gold");
    AudioController.previewSound(FIRST);
    check(AudioController.previewing(FIRST), "music audition did not start");
    library.showPlaylist(MusicPlaylist.BETA);
    int y = trackY(library, FIRST); var row = (MusicRowLayout)call(library, "rowLayout");
    String selected = AudioConfig.current().playlist;
    click(options, row.playX() + 5, y + row.controlsY() + 7, 0);
    check(AudioController.previewPaused() && AudioConfig.current().playlist.equals(selected), "foreign playlist preview pause changed world selection");
    shot(mc, options, "check-preview-paused", LARGE, -1, -1);
    check(trackInk("check-preview-paused", library, FIRST, 0x77baff), "paused preview name lost blue");
    click(options, row.playX() + 5, y + row.controlsY() + 7, 0);
    check(!AudioController.previewPaused() && AudioConfig.current().playlist.equals(selected), "preview resume changed selected playlist");
    library.showTracks(); expand(library); y = trackY(library, FIRST);
    options.render(row.volumeX() + 5, y + row.controlsY() + 7, 0);
    check(library.hoverHelp().isEmpty(), "track volume tooltip still appears");
    AudioController.quiet();
  }

  private static void navigation(Minecraft mc, Release462Checks.State state) throws Exception {
    var options = state.open(LARGE, CUSTOM, false); var library = library(options);
    selector(options, 1); check(library.playlistEditor() && draft(library).id().equals(CUSTOM), "selector secondary click did not open saved active playlist");
    library.cancelPlaylist(); selector(options, 0); Object popup = get(options, "choices");
    @SuppressWarnings("unchecked") List<String> labels = (List<String>)get(popup, "labels");
    int index = labels.indexOf("Beta 1.7.3");
    click(options, number(popup, "x") + 10, popupY(popup, index) + 5, 1);
    check(library.playlistEditor() && draft(library).id().equals(MusicPlaylist.BETA) && AudioConfig.current().playlist.equals(CUSTOM), "selector item secondary click selected foreign playlist instead of viewing it");
    library.cancelPlaylist(); library.showPlaylist(CUSTOM); Object panel = panel(library);
    shot(mc, options, "check-child-plain", LARGE, -1, -1);
    click(options, n(panel, "addX") + 5, n(panel, "actionY") + 5, 0);
    Object child = get(panel, "choices");
    check((boolean)call(child, "active"), "Add songs menu did not open");
    int mx = number(child, "x") + 10, my = popupY(child, 0) + 5;
    shot(mc, options, "check-child-hover", LARGE, mx, my);
    check(pixel("check-child-hover", LARGE, number(child, "x") + 2, popupY(child, 0) + 2) == 0x444444, "child popup lost real hover pointer");
    check(library.hoverHelp().isEmpty(), "child popup exposed underlying track tooltip");
    int outside = n(library, "left") + 1;
    check(pixel("check-child-hover", LARGE, outside, my) == pixel("check-child-plain", LARGE, outside, my),
        "Add songs hover shaded the underlying row");
    choose(options, child, 1); check((boolean)call(child, "active"), "From playlist menu did not replace Add songs menu");
    mx = number(child, "x") + 10; my = popupY(child, 0) + 5;
    shot(mc, options, "check-source-hover", LARGE, mx, my);
    check(pixel("check-source-hover", LARGE, number(child, "x") + 2, popupY(child, 0) + 2) == 0x444444, "source popup lost native hover");
    check(pixel("check-source-hover", LARGE, outside, my) == pixel("check-child-plain", LARGE, outside, my),
        "From playlist hover shaded the underlying row");
    key(options, Keyboard.KEY_ESCAPE);
  }

  private static void waiting(Minecraft mc, Release462Checks.State state) throws Exception {
    var options = state.open(LARGE, CUSTOM, false); var library = library(options);
    gap(mc, options, library, true);
  }

  private static void gap(Minecraft mc, PowerOptionsScreen options, MusicLibraryScreen library, boolean acceptance) throws Exception {
    SilentAudioChecks.ready(mc);
    float volume = mc.options.musicVolume;
    SoundManagerAccessor sound = (SoundManagerAccessor)(Object)mc.soundManager;
    int countdown = sound.power$countdown();
    try {
      AudioController.quiet();
      var config = AudioConfig.copy(); config.waitBetweenTracks = config.delayQueuedTracks = true; config.shuffle = false;
      AudioConfig.preview(config); MusicRequests.edit(queue -> queue.tracks.clear());
      set(AudioController.class, "historyIndex", ((List<?>)get(AudioController.class, "history")).size() - 1);
      set(AudioController.class, "upcoming", null); set(AudioController.class, "nextKey", null);
      sound.power$countdown(1200); mc.options.musicVolume = .01f;
      set(options, "libraryOpen", false); call(options, "layout");
      shot(mc, options, "waiting-gap", LARGE, -1, -1);
      if (!acceptance) return;
      check(AudioController.currentTrackId().isEmpty() && AudioController.remainingDelay() == 1200
          && AudioController.status().contains("until"), "fixture did not enter an actual waiting gap");
      Object status = get(options, "musicStatus");
      click(options, number(status, "labelX") + 5, number(status, "top") + 6, 0);
      check(library.playlistEditor() && draft(library).id().equals(CUSTOM), "waiting status did not navigate to the saved playlist");
      library.cancelPlaylist();
      String outsider = "music:creative2.ogg";
      MusicRequests.edit(queue -> { queue.tracks.clear(); queue.add(outsider); });
      set(options, "libraryOpen", false); call(options, "layout");
      shot(mc, options, "waiting-queued-outsider", LARGE, -1, -1);
      check(AudioController.navigationTrackId().equals(outsider) && AudioController.navigationPlaylistId().isEmpty(), "queued outsider gained a playlist origin");
      click(options, number(status, "labelX") + 5, number(status, "top") + 6, 0);
      check(!library.playlistEditor() && get(library, "folder").equals("library"), "queued outsider status did not open Library");
      check(MusicRequests.tracks().equals(List.of(outsider)), "status navigation consumed the queued track");
      options.render(-1, -1, 0);
      click(options, number(status, "labelX") + 5, number(status, "top") + 6, 1);
      check(library.queueVisible(), "status secondary click did not open nonempty queue");
      MusicRequests.edit(queue -> queue.tracks.clear()); options.render(-1, -1, 0);
      click(options, number(status, "labelX") + 5, number(status, "top") + 6, 1);
      check(!(boolean)get(options, "libraryOpen") && get(options, "page").equals("Audio"), "empty status secondary click did not return to Audio");
    } finally { AudioController.quiet(); mc.options.musicVolume = volume; sound.power$countdown(countdown); }
  }

  private static MusicLibraryScreen library(PowerOptionsScreen options) throws Exception { call(options, "editMusic"); return (MusicLibraryScreen)get(options, "library"); }
  private static Object panel(MusicLibraryScreen library) throws Exception { return get(library, "presets"); }
  private static MusicPresetDraft draft(MusicLibraryScreen library) throws Exception { return (MusicPresetDraft)call(panel(library), "draft"); }
  @SuppressWarnings("unchecked") private static List<MusicPlaylist> playlists(MusicLibraryScreen library) throws Exception { return (List<MusicPlaylist>)call(panel(library), "filteredPresets"); }
  private static List<String> ids(MusicLibraryScreen library) throws Exception { return playlists(library).stream().map(MusicPlaylist::id).toList(); }
  private static int catalogY(MusicLibraryScreen library, String id) throws Exception {
    Object panel = panel(library); List<String> ids = ids(library); int index = ids.indexOf(id); check(index >= 0, "missing catalog playlist " + id);
    int height = n(panel, "rowHeight"), top = n(panel, "rowTop"), bottom = n(panel, "bottom");
    int scroll = Math.max(0, index * height - Math.max(0, bottom - top - height)); set(panel, "scroll", scroll);
    return top + index * height - scroll;
  }
  private static void context(PowerOptionsScreen options, MusicLibraryScreen library, String id) throws Exception {
    Object layout = call(panel(library), "catalogLayout"); click(options, n(layout, "nameX") + 5, catalogY(library, id) + 10, 1);
  }
  private static void duplicate(PowerOptionsScreen options, MusicLibraryScreen library, String id) throws Exception {
    Object layout = call(panel(library), "catalogLayout"); int row = catalogY(library, id);
    click(options, n(layout, "duplicateX") + 5, n(layout, "controlsY", new Class<?>[]{int.class}, row) + 5, 0);
  }
  private static void rename(PowerOptionsScreen options, MusicLibraryScreen library, String id) throws Exception { context(options, library, id); choose(options, get(options, "choices"), 1); }
  private static void choose(PowerOptionsScreen options, Object popup, int index) throws Exception { click(options, number(popup, "x") + 10, popupY(popup, index) + 6, 0); }
  private static void selector(PowerOptionsScreen options, int button) throws Exception { Object bounds = call(options, "playlistButton"); click(options, n(bounds, "x") + 5, n(bounds, "y") + 5, button); }
  private static void query(MusicLibraryScreen library, String text) throws Exception { ((TextInput)get(library, "search")).setText(text); call(library, "rebuild"); }
  private static void expand(MusicLibraryScreen library) throws Exception { ((Set<?>)get(library, "collapsed")).clear(); call(library, "rebuildRows"); }
  private static void grouped(MusicLibraryScreen library) throws Exception { set(panel(library), "grouped", true); call(library, "syncPanel"); expand(library); }
  private static List<BuiltinMusic.Track> moogTracks() {
    var exact = BuiltinMusic.TRACKS.stream().filter(track -> track.title().equals("Moog City")).findFirst().orElseThrow();
    var weaker = BuiltinMusic.TRACKS.stream().filter(track -> track.title().equals(exact.title() + " 2")).findFirst().orElseThrow();
    return List.of(exact, weaker);
  }
  private static void forceFirstGroup(MusicLibraryScreen library, String group) throws Exception {
    @SuppressWarnings("unchecked") var order = new ArrayList<>((List<String>)get(library, "groupOrder"));
    order.remove(group); order.add(0, group); set(library, "groupOrder", List.copyOf(order));
  }
  private static int trackY(MusicLibraryScreen library, String id) throws Exception {
    for (Object row : (List<?>)get(library, "rows")) if (id.equals(call(row, "id"))) return reveal(library, row);
    throw new AssertionError("missing track " + id);
  }
  private static int groupY(MusicLibraryScreen library, String group) throws Exception {
    for (Object row : (List<?>)get(library, "rows")) if (call(row, "id") == null && group.equals(call(row, "group"))) return reveal(library, row);
    throw new AssertionError("missing group " + group);
  }
  private static int reveal(MusicLibraryScreen library, Object row) throws Exception {
    int natural = n(row, "y"); call(library, "scroll", new Class<?>[]{int.class}, Math.max(0, natural - 20));
    return n(library, "listTop") + natural - n(library, "scroll");
  }
  private static String uncheckedGroup(MusicLibraryScreen library, String id) {
    try { return (String)call(library, "group", new Class<?>[]{String.class}, id); }
    catch (Exception failure) { throw new RuntimeException(failure); }
  }
  private static int popupY(Object popup, int index) throws Exception { return number(popup, "y") + n(get(popup, "layout"), "top", new Class<?>[]{int.class}, index) - ((Number)get(popup, "scroll")).intValue(); }
  private static void click(Screen screen, int x, int y, int button) { ((ScreenInput)(Object)screen).power$click(x, y, button); }
  private static void release(Screen screen, int x, int y, int button) { ((ScreenInput)(Object)screen).power$release(x, y, button); }
  private static void key(Screen screen, int key) { ((ScreenInput)(Object)screen).power$key('\0', key); }
  private static void shot(Minecraft mc, Screen screen, String label, int[] size, int mx, int my) throws Exception { Files.createDirectories(REPORT); SettingsSnapshot.render(mc, screen, size[0], size[1], REPORT.resolve(label + "-" + size[0] + ".png"), mx, my); }
  private static BufferedImage image(String label, int[] size) throws Exception { return ImageIO.read(REPORT.resolve(label + "-" + size[0] + ".png").toFile()); }
  private static int pixel(String label, int[] size, int x, int y) throws Exception { return image(label, size).getRGB(x * 2, y * 2) & 0xffffff; }
  private static boolean ink(String label, int[] size, int x, int y, int width, int height, int color) throws Exception {
    var image = image(label, size);
    for (int py = y * 2; py < (y + height) * 2; py++) for (int px = x * 2; px < (x + width) * 2; px++)
      if ((image.getRGB(px, py) & 0xffffff) == color) return true;
    return false;
  }
  private static boolean trackInk(String label, MusicLibraryScreen library, String id, int color) throws Exception { var row = (MusicRowLayout)call(library, "rowLayout"); return ink(label, LARGE, row.nameX(), trackY(library, id) + 7, row.nameWidth(), 8, color); }
  private static int number(Object owner, String name) throws Exception { return ((Number)get(owner, name)).intValue(); }
  private static int n(Object owner, String name) throws Exception { return ((Number)call(owner, name)).intValue(); }
  private static int n(Object owner, String name, Class<?>[] types, Object... args) throws Exception { return ((Number)call(owner, name, types, args)).intValue(); }
  private static Field field(Class<?> type, String name) throws Exception { for (; type != null; type = type.getSuperclass()) try { var field = type.getDeclaredField(name); field.setAccessible(true); return field; } catch (NoSuchFieldException ignored) {} throw new NoSuchFieldException(name); }
  private static Object get(Object owner, String name) throws Exception { return field(owner instanceof Class<?> type ? type : owner.getClass(), name).get(owner instanceof Class<?> ? null : owner); }
  private static void set(Object owner, String name, Object value) throws Exception { field(owner instanceof Class<?> type ? type : owner.getClass(), name).set(owner instanceof Class<?> ? null : owner, value); }
  private static Object call(Object owner, String name) throws Exception { return call(owner, name, new Class<?>[0]); }
  private static Object call(Object owner, String name, Class<?>[] types, Object... args) throws Exception {
    for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) try { var method = type.getDeclaredMethod(name, types); method.setAccessible(true); return method.invoke(owner, args); } catch (NoSuchMethodException ignored) {}
    throw new NoSuchMethodException(name);
  }

  /** Quiet test streams first, then restore playback identity and selector bookkeeping. */
  private static final class PlaybackState implements AutoCloseable {
    private final Map<Field,Object> fields = new LinkedHashMap<>();
    private final Map<Field,List<?>> lists = new LinkedHashMap<>();
    private final SoundManagerAccessor sound;
    private final int countdown;
    private final Object selector;
    private final Object selectorPrevious;
    private final Map<?,?> cursors;
    private final Map<Object,Map<Field,Object>> cursorValues = new IdentityHashMap<>();
    PlaybackState(Minecraft mc) throws Exception {
      sound = (SoundManagerAccessor)(Object)mc.soundManager; countdown = sound.power$countdown();
      for (Class<?> type : List.of(AudioController.class, Class.forName("local.luke.power.audio.MusicPreview")))
        for (Field field : type.getDeclaredFields()) {
          if (!Modifier.isStatic(field.getModifiers())) continue;
          field.setAccessible(true);
          if (!Modifier.isFinal(field.getModifiers())) fields.put(field, field.get(null));
          else if (field.get(null) instanceof List<?> value) lists.put(field, new ArrayList<>(value));
        }
      selector = get(AudioController.class, "WORLD"); selectorPrevious = get(selector, "previous");
      cursors = new LinkedHashMap<>((Map<?,?>)get(selector, "cursors"));
      for (Object cursor : cursors.values()) {
        Map<Field,Object> values = new LinkedHashMap<>();
        for (Field field : cursor.getClass().getDeclaredFields()) { field.setAccessible(true); values.put(field, field.get(cursor)); }
        cursorValues.put(cursor, values);
      }
      for (Object progress : List.of(get(AudioController.class, "progress"), get(Class.forName("local.luke.power.audio.MusicPreview"), "progress"))) {
        Map<Field,Object> values = new LinkedHashMap<>();
        for (Field field : progress.getClass().getDeclaredFields()) { field.setAccessible(true); values.put(field, field.get(progress)); }
        cursorValues.put(progress, values);
      }
    }
    @SuppressWarnings({"rawtypes", "unchecked"}) public void close() throws Exception {
      AudioController.quiet();
      for (var entry : fields.entrySet()) entry.getKey().set(null, entry.getValue());
      for (var entry : lists.entrySet()) { List values = (List)entry.getKey().get(null); values.clear(); values.addAll(entry.getValue()); }
      set(selector, "previous", selectorPrevious);
      Map current = (Map)get(selector, "cursors"); current.clear(); current.putAll(cursors);
      for (var values : cursorValues.entrySet()) for (var entry : values.getValue().entrySet()) entry.getKey().set(values.getKey(), entry.getValue());
      sound.power$countdown(countdown);
    }
  }
}
