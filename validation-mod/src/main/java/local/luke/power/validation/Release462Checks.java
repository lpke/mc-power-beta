package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.recipes.*;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/** Native release review. Capture runs independently so failed acceptance cannot hide a changed view. */
final class Release462Checks {
  private static final Path REPORT = Path.of("power-beta-data/reports/release-462/ui");
  private static final int[][] SIZES = {{854, 480}, {320, 240}};
  private static final String FIRST = "music:calm1.ogg", SECOND = "music:calm2.ogg", THIRD = "music:creative1.ogg";
  private static final String CUSTOM = "ui462-long", LONG_NAME = "Quiet afternoons and late evening building sessions with friends";
  private static final String[] ACTIVE = {MusicPlaylist.BETA, CUSTOM, MusicPlaylist.FAVORITES};
  private Release462Checks() {}

  static void run(Minecraft mc, String action) throws Exception {
    check(action.equals("capture") || action.equals("check"), "use release-462 capture|check");
    SilentAudioChecks.ready(mc);
    check(mc.world != null && mc.player != null && !mc.world.isRemote, "open a disposable singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable client");
    failures = 0;
    try (var state = new State(mc)) {
      if (action.equals("capture")) capture(mc, state);
      else {
        test("RELEASE462 active playlist tabs open the selected draft and protect dirty exits", () -> navigation(mc, state));
        test("RELEASE462 active selection, rename, deletion and Favorites refresh retain draft ownership", () -> lifecycle(mc, state));
        test("RELEASE462 Extra sounds starts closed and searched category labels lose indentation", () -> options(mc, state));
        test("RELEASE462 group X/Y counts keep their position and membership during search", () -> groups(mc, state));
        test("RELEASE462 Add edits one isolated song and search keeps group membership counts", () -> additions(mc, state));
        test("RELEASE462 selected dropdown hover, divider and Settings-style song hover", () -> pixels(mc, state));
        test("RELEASE462 playing and auditioned names are gold, paused names are ordinary", () -> playback(mc, state));
        test("RELEASE462 color and standalone recipe spacing retain native inventory bounds", () -> spacing(mc, state));
      }
    }
    log("RELEASE462 " + action + " failures=" + failures);
    check(failures == 0, "release-462 failures=" + failures);
  }

  private static AudioSettings seed(String active) {
    var audio = new AudioSettings();
    audio.playlists.add(new MusicPlaylist(CUSTOM, LONG_NAME, List.of(FIRST, SECOND, THIRD), Map.of("Alpha", 40)));
    audio.playlists.add(new MusicPlaylist("ui462-empty", "Empty playlist", List.of(), Map.of()));
    audio.favourites.add(FIRST); audio.sounds.put(FIRST, 35);
    MusicPlaylist.load(audio, active); MusicPlaylist.loadMenu(audio, MusicPlaylist.SAME_AS_WORLD);
    return audio;
  }

  private static void capture(Minecraft mc, State state) throws Exception {
    Files.createDirectories(REPORT); var names = new ArrayList<String>();
    for (int[] size : SIZES) {
      for (String active : ACTIVE) for (boolean queued : new boolean[] {false, true}) {
        String prefix = active + (queued ? "-queue" : "-empty-queue");
        var views = active.equals(CUSTOM) ? List.of("active", "catalog", "library-hover", "view", "grouped", "add-songs", "selector")
            : active.equals(MusicPlaylist.FAVORITES) && !queued ? List.of("active", "grouped") : List.of("active");
        for (String view : views) {
          var options = state.open(size, active, queued); var library = library(options);
          int mx = -1, my = -1;
          switch (view) {
            case "active" -> active(library);
            case "catalog" -> library.openPresets();
            case "library-hover" -> { library.showTrack(FIRST); mx = n(library, "left") + 5; my = trackY(library, FIRST) + 4; }
            case "selector" -> selector(options);
            default -> {
              edit(library, active); Object panel = get(library, "presets");
              if (view.equals("view")) click(options, n(panel, "groupedX") + 5, n(panel, "actionY") + 8, 0);
              else if (view.equals("grouped")) { set(panel, "grouped", true); call(library, "syncPanel"); expand(library); }
              else if (!draft(library).readOnly()) { adding(options, library); expand(library); }
            }
          }
          shot(mc, options, prefix + "-" + view, size, mx, my, names);
        }
      }
      var options = state.open(size, CUSTOM, false);
      ((Set<String>)get(options, "collapsed")).addAll(SettingsLayout.initiallyCollapsed()); call(options, "layout");
      shot(mc, options, "audio-options", size, -1, -1, names);
      ((TextInput)get(options, "search")).setText("Game sounds"); call(options, "layout");
      shot(mc, options, "search-categories", size, -1, -1, names);
      spacingShots(mc, state, size, names);
    }
    Files.writeString(REPORT.resolve("captures.json"), Catalog.JSON.toJson(names));
    log("RELEASE462 captures=" + names.size() + " directory=" + REPORT.toAbsolutePath());
  }

  private static void navigation(Minecraft mc, State state) throws Exception {
    check(MusicPlaylist.beta().name().equals("Beta 1.7.3"), "built-in name lost its version");
    for (int[] size : SIZES) for (String id : ACTIVE) for (boolean queue : new boolean[]{false, true}) {
      var options = state.open(size, id, queue); var library = library(options); library.showTracks();
      var filters = (MusicFilters.Layout)call(library, "filters");
      var tab = filters.tabs().stream().filter(t -> t.id().equals("active")).findFirst().orElseThrow();
      click(options, tab.x() + 7, tab.y() + 7, 0);
      check(draft(library) != null && draft(library).id().equals(id), "active tab did not open selected playlist " + id);
      check(draft(library).readOnly() == MusicPlaylist.BETA.equals(id), "wrong Beta/Favorites editability");
      check(n(library, "bottom") - n(library, "listTop") >= 24, "active view cannot show one complete song");
      if (draft(library).readOnly()) {
        boolean back = false;
        for (Object button : (List<?>)call(options, "draftFooter")) if (call(button, "label").equals("Back")) {
          click(options, (int)call(button, "x") + 5, size[1] - 20, 0); back = true; break;
        }
        check(back && !(boolean)call(options, "libraryVisible"), "read-only active Back did not return to Audio settings");
      } else {
        var original = MusicPlaylist.find(AudioConfig.current(), id).tracks(); draft(library).include(FIRST, false); call(library, "syncPanel");
        var target = ((MusicFilters.Layout)call(library, "filters")).tabs().stream().filter(t -> t.id().equals("library")).findFirst().orElseThrow();
        click(options, target.x() + 5, target.y() + 5, 0);
        check(library.modal() && library.playlistDirty(), "dirty active tab silently left its draft");
        check(MusicPlaylist.find(AudioConfig.current(), id).tracks().equals(original), "dirty navigation published membership");
        key(options, Keyboard.KEY_ESCAPE); library.cancelPlaylist();
      }
    }
    var options = state.open(SIZES[0], CUSTOM, false); var library = library(options);
    AudioController.playNow(FIRST); state.playback = true; AudioController.pause();
    options.render(-1, -1, 0); Object status = get(options, "musicStatus");
    click(options, (int)get(status, "labelX") + 4, (int)get(status, "top") + 6, 0);
    check(draft(library) != null && draft(library).id().equals(CUSTOM), "song-name jump opened Library instead of active playlist");
  }

  private static void additions(Minecraft mc, State state) throws Exception {
    for (int[] size : SIZES) for (boolean queued : new boolean[]{false, true}) {
      var options = state.open(size, CUSTOM, queued); var library = library(options); edit(library, CUSTOM);
      draft(library).include(SECOND, false); call(library, "syncPanel");
      var persisted = MusicPlaylist.find(AudioConfig.current(), CUSTOM).tracks(); adding(options, library);
      query(library, "minecraft"); expand(library);
      @SuppressWarnings("unchecked") var ids = (List<String>)get(library, "tracks");
      check(ids.stream().allMatch(id -> AudioController.musicLabel(id).toLowerCase(Locale.ROOT).contains("minecraft")), "track filter matched hidden membership or metadata");
      query(library, "mnecraft"); check(((List<?>)get(library, "tracks")).isEmpty(), "song search fuzzy-matched");
      query(library, ""); expand(library); int y = trackY(library, SECOND);
      String playing = AudioController.currentTrackId(); var queueBefore = MusicRequests.tracks();
      var add = (MusicRowLayout)call(library, "rowLayout");
      click(options, add.nameX() + 4, y + add.controlsY() + 7, 0);
      check(draft(library).included(SECOND), "single-song Add missed its native hit target");
      check(MusicPlaylist.find(AudioConfig.current(), CUSTOM).tracks().equals(persisted), "single-song Add published before Save");
      check(AudioController.currentTrackId().equals(playing) && MusicRequests.tracks().equals(queueBefore), "single-song Add changed playback or queue");
      check(n(library, "bottom") - n(library, "listTop") >= 44, "small Add songs view lost a heading/full row");
      library.cancelPlaylist(); check(MusicPlaylist.find(AudioConfig.current(), CUSTOM).tracks().equals(persisted), "Cancel retained single-song Add");
    }
  }

  private static void lifecycle(Minecraft mc, State state) throws Exception {
    var options = state.open(SIZES[0], CUSTOM, false); var library = library(options); active(library);
    Object edits = get(library, "edits"); var owner = draft(library); owner.include(SECOND, false); call(library, "syncPanel");
    selector(options); key(options, Keyboard.KEY_HOME); key(options, Keyboard.KEY_DOWN); key(options, Keyboard.KEY_RETURN);
    check(library.modal() && AudioConfig.current().playlist.equals(CUSTOM) && draft(library) == owner,
        "toolbar selection bypassed the dirty playlist Save/Discard prompt");
    int dialogWidth = Math.min(312, options.width - 20), dialogLeft = (options.width - dialogWidth) / 2;
    click(options, dialogLeft + 2 * (dialogWidth / 3) + 8, options.height / 2 + 9, 0);
    check(!library.modal() && draft(library) == owner && library.playlistDirty() && AudioConfig.current().playlist.equals(CUSTOM),
        "Keep editing replaced the draft or changed the selected playlist");
    call(edits, "load", new Class<?>[]{String.class}, MusicPlaylist.BETA); library.tick();
    check(draft(library) == owner && owner.changed(LONG_NAME), "active selection change replaced an unsaved draft");
    check(call(library, "activeTabLabel").equals(owner.name()), "external selection change mislabeled the retained dirty draft");
    options = state.open(SIZES[0], CUSTOM, false); library = library(options); active(library); edits = get(library, "edits");
    String renamed = "Renamed quiet afternoons";
    call(edits, "save", new Class<?>[]{MusicPlaylist.class}, new MusicPlaylist(CUSTOM, renamed, List.of(FIRST, SECOND, THIRD), Map.of("Alpha", 40))); library.tick();
    var tabs = (MusicFilters.Layout)call(library, "filters");
    check(tabs.tabs().stream().anyMatch(t -> t.id().equals("active") && t.label().equals(renamed)) && draft(library).name().equals(renamed), "saved active rename did not update tab and clean draft");
    call(edits, "delete", new Class<?>[]{String.class}, CUSTOM); library.tick();
    check(AudioConfig.current().playlist.equals(MusicPlaylist.BETA) && draft(library).id().equals(MusicPlaylist.BETA), "deleting selected playlist did not fall back to Beta");
    options = state.open(SIZES[0], MusicPlaylist.FAVORITES, false); library = library(options); active(library); edits = get(library, "edits");
    var favorites = new MusicPlaylist(MusicPlaylist.FAVORITES, "Favorites", List.of(SECOND, FIRST), Map.of("Alpha", 25));
    call(edits, "save", new Class<?>[]{MusicPlaylist.class}, favorites); library.tick();
    check(draft(library).tracks().equals(favorites.tracks()) && draft(library).volume("Alpha") == 25, "active Favorites refresh lost membership or gains");
    options = state.open(SIZES[0], CUSTOM, false); library = library(options); active(library); var before = draft(library).tracks();
    library.showActiveTrack("music:creative2.ogg");
    check(draft(library).tracks().equals(before) && !((String)get(library, "error")).isEmpty(), "outside-song jump appended a member or lost its notice");
  }

  private static void options(Minecraft mc, State state) throws Exception {
    check(SettingsLayout.initiallyCollapsed().contains("Audio/Extra sounds"), "Extra sounds is not closed by default");
    for (int[] size : SIZES) {
      var options = state.open(size, CUSTOM, false);
      @SuppressWarnings("unchecked") var collapsed = (Set<String>)get(options, "collapsed");
      collapsed.addAll(SettingsLayout.initiallyCollapsed()); call(options, "layout");
      for (Object row : (List<?>)get(options, "rows")) { var value = (Setting)call(row, "setting"); check(value == null || !value.group.equals("Extra sounds"), "closed Extra sounds still renders its children"); }
      for (Object volumeRow : (List<?>)get(options, "rows")) {
        var volume = (Setting)call(volumeRow, "setting"); if (volume == null) continue;
        if (volume.id.equals("audio.master") || volume.id.startsWith("audio.category.") || volume.id.equals("native.music") || volume.id.equals("native.sound")) {
          int expectedOffset = volume.id.startsWith("native.") ? 48 : 0;
          check((int)call(options, "mainControlLeft", new Class<?>[]{volumeRow.getClass()}, volumeRow) == (int)call(options, "controlLeft", new Class<?>[]{volumeRow.getClass()}, volumeRow) + expectedOffset, "volume slider gained unwanted indentation: " + volume.id);
        }
      }
      ((TextInput)get(options, "search")).setText("ambient"); call(options, "layout");
      Object row = null;
      for (Object candidate : (List<?>)get(options, "rows")) { var value = (Setting)call(candidate, "setting"); if (value != null && value.id.equals("audio.category.ambient")) { row = candidate; break; } }
      check(row != null, "search lost the Ambient category");
      set(options, "scroll", Math.max(0d, n(row, "y") - 24d)); call(options, "layout");
      int x = n(options, "left") + 3, y = n(options, "top") + n(row, "y") - ((Number)get(options, "scroll")).intValue() + (n(row, "height") == 40 ? 3 : 8);
      String label = (String)call(options, "settingLabel", new Class<?>[]{Setting.class}, setting(options, "audio.category.ambient"));
      var reference = new CountReference(label, x, y, 0xc4c4c4); reference.init(mc, size[0], size[1]);
      shot(mc, reference, "category-reference", size, -1, -1, null);
      shot(mc, options, "category-no-indent", size, -1, -1, null);
      checkTextMask("category-no-indent", "category-reference", size, x, y, Math.min(48, mc.textRenderer.getWidth(label)), 8, 0xc4c4c4);
    }
  }

  private static void groups(Minecraft mc, State state) throws Exception {
    for (int[] size : SIZES) {
      var options = state.open(size, CUSTOM, false); var library = library(options); edit(library, CUSTOM);
      Object panel = get(library, "presets"); set(panel, "grouped", true); call(library, "syncPanel"); expand(library);
      var row = (MusicRowLayout)call(library, "rowLayout");
      check(n(library, "groupVolumeX") == row.volumeX() && n(library, "groupVolumeWidth") == row.volumeWidth(), "group gain no longer shares child slider bounds");
      @SuppressWarnings("unchecked") var totals = Map.copyOf((Map<String,Integer>)get(library, "libraryGroupCounts"));
      int included = (int)call(library, "includedCount", new Class<?>[]{String.class}, "Alpha");
      check(included == 2 && totals.getOrDefault("Alpha", 0) >= included, "fixture Alpha membership is not available");
      String label = included + "/" + totals.get("Alpha");
      int right = n(library, "right") - 4, x = right - mc.textRenderer.getWidth(label), y = n(library, "listTop") + 6;
      var reference = new CountReference(label, x, y); reference.init(mc, size[0], size[1]);
      shot(mc, reference, "count-reference", size, -1, -1, null);
      shot(mc, options, "group-count", size, -1, -1, null);
      checkTextMask("group-count", "count-reference", size, x, y, right - x, 8);
      query(library, "Minecraft"); expand(library);
      check((int)call(library, "includedCount", new Class<?>[]{String.class}, "Alpha") == included && totals.equals(get(library, "libraryGroupCounts")), "search changed membership numerator/denominator");
      shot(mc, options, "group-count-search", size, -1, -1, null);
      checkTextMask("group-count-search", "count-reference", size, x, y, right - x, 8);
    }
  }

  private static void pixels(Minecraft mc, State state) throws Exception {
    for (int[] size : SIZES) {
      var options = state.open(size, CUSTOM, false); selector(options); Object choices = get(options, "choices");
      int selected = (int)get(choices, "selected"), x = (int)get(choices, "x"), y = popupY(choices, selected);
      shot(mc, options, "selected-hover", size, x + 12, y + 2, null);
      check(pixel("selected-hover", size, x + 1, y + 1) == 0x444444, "selected popup hover stayed darker than normal hover");
      Object layout = get(choices, "layout");
      @SuppressWarnings("unchecked") var separators = (Set<Integer>)call(layout, "separators");
      for (int index : separators) {
        int line = popupY(choices, index) - 1;
        check((int)call(layout, "top", new Class<?>[]{int.class}, index) - (int)call(layout, "top", new Class<?>[]{int.class}, index - 1) == (int)get(choices, "rowHeight") + 1,
            "playlist divider adds vertical gaps");
        check(pixel("selected-hover", size, x + 1, line) == (PopupStyle.SEPARATOR & 0xffffff), "playlist divider lost its one-pixel dull line");
      }
      key(options, Keyboard.KEY_ESCAPE); var library = library(options); library.showTrack(FIRST); int rowY = trackY(library, FIRST);
      shot(mc, options, "settings-song-plain", size, -1, -1, null);
      shot(mc, options, "settings-song-hover", size, n(library, "left") + 5, rowY + 4, null);
      int plain = pixel("settings-song-plain", size, n(library, "left"), rowY + 1);
      int hovered = pixel("settings-song-hover", size, n(library, "left"), rowY + 1);
      for (int shift : new int[]{0,8,16}) check(Math.abs((hovered >> shift & 255) - (plain >> shift & 255) * 159d / 255) <= 1, "song hover differs from Settings row fill");
      library.openPresets(); Object panel = get(library, "presets");
      shot(mc, options, "playlist-no-tooltip", size, n(panel, "left") + 5, n(panel, "rowTop") + 5, null);
      check(library.hoverHelp().isEmpty(), "playlist rows still show tooltips");
    }
  }

  private static void playback(Minecraft mc, State state) throws Exception {
    var options = state.open(SIZES[0], CUSTOM, false); var library = library(options); library.showTrack(FIRST);
    AudioController.playNow(FIRST); state.playback = true;
    shot(mc, options, "playing-gold", SIZES[0], -1, -1, null);
    check(gold("playing-gold", SIZES[0], library, FIRST), "playing song name has no gold pixels");
    AudioController.pause(); shot(mc, options, "paused-ordinary", SIZES[0], -1, -1, null);
    check(!gold("paused-ordinary", SIZES[0], library, FIRST), "paused song name stays gold");
    AudioController.previewSound(FIRST); shot(mc, options, "audition-gold", SIZES[0], -1, -1, null);
    check(gold("audition-gold", SIZES[0], library, FIRST), "auditioned song name has no gold pixels");
    AudioController.pause(); shot(mc, options, "audition-paused", SIZES[0], -1, -1, null);
    check(!gold("audition-paused", SIZES[0], library, FIRST), "paused audition stays gold");
    AudioController.quiet();
  }

  private static void spacing(Minecraft mc, State state) throws Exception {
    for (int[] size : SIZES) {
      var options = state.open(size, CUSTOM, false);
      var picker = new ColourScreen(options, setting(options, "maps.destinationArrowColor"), setting(options, "maps.destinationArrowOpacity"), () -> {});
      mc.setScreen(picker); picker.init(mc, size[0], size[1]);
      check(n(picker, "tabsY") - n(picker, "previewY") - 36 == 10, "color preview gap is not 10px");
      check(n(picker, "bodyTop") < n(picker, "bodyBottom"), "small color body lost its viewport");
      var menu = new RecipeBookLayout(size[0], size[1], true, true);
      check(menu.kindTab(0).y() >= 22 && menu.y() - menu.kindTab(0).y() - 20 >= 4, "recipe menu tabs collide with title or panel");
      var overlay = new RecipeBookLayout(size[0], size[1], true, false);
      check(overlay.y() == (size[1] - 166) / 2 && overlay.kindTab(0).y() == overlay.y() - 22, "menu spacing changed native inventory overlay");
    }
  }

  private static void spacingShots(Minecraft mc, State state, int[] size, List<String> names) throws Exception {
    var options = state.open(size, CUSTOM, false);
    var picker = new ColourScreen(options, setting(options, "maps.destinationArrowColor"), setting(options, "maps.destinationArrowOpacity"), () -> {});
    mc.setScreen(picker); picker.init(mc, size[0], size[1]);
    for (int tab = 0; tab < 2; tab++) { picker.layerPress(n(picker, "left") + tab * (n(picker, "tabWidth") + 4) + 5, n(picker, "tabsY") + 5, 0); shot(mc, picker, "color-" + tab, size, -1, -1, names); }
    var viewer = new RecipeBookViewerScreen(options); mc.setScreen(viewer); viewer.init(mc, size[0], size[1]);
    for (int tab = 0; tab < 2; tab++) { var bounds = viewer.power$recipeBook().layout().kindTab(tab); viewer.layerPress(bounds.x() + 5, bounds.y() + 5, 0); shot(mc, viewer, "recipe-" + tab, size, -1, -1, names); }
  }

  private static MusicLibraryScreen library(PowerOptionsScreen options) throws Exception { call(options, "editMusic"); return (MusicLibraryScreen)get(options, "library"); }
  private static void active(MusicLibraryScreen library) throws Exception { call(library, "selectFilter", new Class<?>[]{String.class}, "active"); }
  private static void edit(MusicLibraryScreen library, String id) throws Exception { library.openPresets(); call(get(library, "presets"), "edit", new Class<?>[]{MusicPlaylist.class}, MusicPlaylist.find(AudioConfig.current(), id)); call(library, "syncPanel"); }
  private static MusicPresetDraft draft(MusicLibraryScreen library) throws Exception { return (MusicPresetDraft)call(get(library, "presets"), "draft"); }
  private static void adding(PowerOptionsScreen options, MusicLibraryScreen library) throws Exception {
    Object panel = get(library, "presets"); click(options, n(panel, "addX") + 6, n(panel, "actionY") + 7, 0);
    key(options, Keyboard.KEY_HOME); key(options, Keyboard.KEY_RETURN); call(library, "syncPanel");
    check((boolean)call(panel, "adding"), "From Library did not enter Add songs");
  }
  private static void selector(PowerOptionsScreen options) throws Exception { Object button = call(options, "playlistButton"); click(options, (int)call(button, "x") + 5, (int)call(button, "y") + 5, 0); }
  private static void query(MusicLibraryScreen library, String text) throws Exception { ((TextInput)get(library, "search")).setText(text); call(library, "rebuild"); }
  private static void expand(MusicLibraryScreen library) throws Exception { ((Set<?>)get(library, "collapsed")).clear(); call(library, "rebuildRows"); }
  private static int trackY(MusicLibraryScreen library, String id) throws Exception {
    for (Object row : (List<?>)get(library, "rows")) if (id.equals(call(row, "id"))) { int natural = n(row, "y"); call(library, "scroll", new Class<?>[]{int.class}, Math.max(0, natural - 20)); return n(library, "listTop") + natural - n(library, "scroll"); }
    throw new AssertionError("missing song " + id);
  }
  private static Setting setting(PowerOptionsScreen options, String id) { return options.session().settings().stream().filter(s -> s.id.equals(id)).findFirst().orElseThrow(); }
  private static int popupY(Object choices, int index) throws Exception { return (int)get(choices, "y") + (int)call(get(choices, "layout"), "top", new Class<?>[]{int.class}, index) - ((Number)get(choices, "scroll")).intValue(); }
  private static void click(Screen screen, int x, int y, int button) { ((ScreenInput)(Object)screen).power$click(x, y, button); }
  private static void key(Screen screen, int key) { ((ScreenInput)(Object)screen).power$key('\0', key); }
  private static void shot(Minecraft mc, Screen screen, String label, int[] size, int x, int y, List<String> names) throws Exception { Files.createDirectories(REPORT); String name = label + "-" + size[0] + ".png"; SettingsSnapshot.render(mc, screen, size[0], size[1], REPORT.resolve(name), x, y); if (names != null) names.add(name); }
  private static BufferedImage image(String label, int[] size) throws Exception { return ImageIO.read(REPORT.resolve(label + "-" + size[0] + ".png").toFile()); }
  private static int pixel(String label, int[] size, int x, int y) throws Exception { return image(label, size).getRGB(x * 2, y * 2) & 0xffffff; }
  private static boolean gold(String label, int[] size, MusicLibraryScreen library, String id) throws Exception {
    var image = image(label, size); var row = (MusicRowLayout)call(library, "rowLayout"); int y = trackY(library, id);
    for (int py = (y + 7) * 2; py < (y + 15) * 2; py++) for (int px = row.nameX() * 2; px < (row.nameX() + row.nameWidth()) * 2; px++) if ((image.getRGB(px, py) & 0xffffff) == 0xffdd66) return true;
    return false;
  }
  private static void checkTextMask(String actual, String reference, int[] size, int x, int y, int width, int height) throws Exception {
    checkTextMask(actual, reference, size, x, y, width, height, 0xaaaaaa);
  }
  private static void checkTextMask(String actual, String reference, int[] size, int x, int y, int width, int height, int color) throws Exception {
    var a = image(actual, size); var b = image(reference, size); int ink = 0;
    for (int py = y * 2; py < (y + height) * 2; py++) for (int px = x * 2; px < (x + width) * 2; px++) {
      boolean expected = (b.getRGB(px, py) & 0xffffff) == color;
      if (expected) ink++;
      check(((a.getRGB(px, py) & 0xffffff) == color) == expected, "native text moved or used a different value");
    }
    check(ink > 0, "count reference has no native font pixels");
  }
  private static final class CountReference extends UiScreen {
    final String label; final int x, y, color;
    CountReference(String label, int x, int y) { this(label,x,y,0xaaaaaa); }
    CountReference(String label, int x, int y, int color) { this.label = label; this.x = x; this.y = y; this.color = color; }
    @Override public void render(int mx, int my, float delta) { fill(0,0,width,height,0xff000000); text(label,x,y,color); }
  }
  private static int n(Object owner, String name) throws Exception { return ((Number)call(owner, name)).intValue(); }
  private static Field field(Class<?> type, String name) throws Exception { for (; type != null; type = type.getSuperclass()) try { var f = type.getDeclaredField(name); f.setAccessible(true); return f; } catch (NoSuchFieldException ignored) {} throw new NoSuchFieldException(name); }
  private static Object get(Object owner, String name) throws Exception { return field(owner.getClass(), name).get(owner); }
  private static void set(Object owner, String name, Object value) throws Exception { field(owner.getClass(), name).set(owner, value); }
  private static Object call(Object owner, String name) throws Exception { return call(owner, name, new Class<?>[0]); }
  private static Object call(Object owner, String name, Class<?>[] types, Object... args) throws Exception { for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) try { var method = type.getDeclaredMethod(name, types); method.setAccessible(true); return method.invoke(owner, args); } catch (NoSuchMethodException ignored) {} throw new NoSuchMethodException(name); }

  static final class State implements AutoCloseable {
    final Minecraft mc; final MusicPlaylists460Checks.State previous;
    final boolean automatic, grabbed; final Object scope; final int guiScale;
    final Field customScale, realScale; final float custom, real; final int matrix;
    final RecipeBookSettings recipes;
    PowerOptionsScreen options; boolean playback;
    State(Minecraft mc) throws Exception {
      this.mc = mc; previous = new MusicPlaylists460Checks.State(mc);
      recipes = RecipeBookConfig.copy(); var enabledRecipes = RecipeBookConfig.copy(); enabledRecipes.enabled = enabledRecipes.pauseMenu = true; RecipeBookConfig.preview(enabledRecipes);
      automatic = MenuPreferences.current().autoApply; grabbed = Mouse.isGrabbed(); guiScale = mc.options.guiScale;
      scope = field(Class.forName("local.luke.power.ui.OptionsSearchScope"), "choice").get(null);
      Class<?> scales = Class.forName("local.luke.power.controls.util.ModOptions"); customScale = scales.getField("guiScale"); realScale = scales.getField("realGuiScale"); custom = customScale.getFloat(null); real = realScale.getFloat(null);
      matrix = GL11.glGetInteger(GL11.GL_MATRIX_MODE); GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
      MenuPreferences.current().autoApply = false; Mouse.setGrabbed(false); mc.options.guiScale = 2; customScale.setFloat(null, .25f); realScale.setFloat(null, .25f);
    }
    PowerOptionsScreen open(int[] size, String id, boolean queued) throws Exception {
      if (options != null) { options.session().discard(); options.removed(); }
      AudioConfig.preview(seed(id)); MusicRequests.edit(queue -> { queue.tracks.clear(); if (queued) queue.add(FIRST); });
      field(Class.forName("local.luke.power.ui.OptionsSearchScope"), "choice").set(null, false);
      options = new PowerOptionsScreen(mc.currentScreen); mc.setScreen(options); options.init(mc, size[0], size[1]); set(options, "page", "Audio"); set(options, "libraryOpen", false); set(options, "allSounds", false); ((TextInput)get(options, "search")).setText(""); ((Set<?>)get(options, "collapsed")).clear(); call(options, "layout"); return options;
    }
    public void close() throws Exception {
      try { if (options != null) { options.session().discard(); options.removed(); } }
      finally { try { if (playback) AudioController.quiet(); RecipeBookConfig.preview(recipes); MenuPreferences.current().autoApply = automatic; field(Class.forName("local.luke.power.ui.OptionsSearchScope"), "choice").set(null, scope); previous.close(); }
      finally { mc.options.guiScale = guiScale; customScale.setFloat(null, custom); realScale.setFloat(null, real); Mouse.setGrabbed(grabbed); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix(); GL11.glPopAttrib(); GL11.glMatrixMode(matrix); }
      }
    }
  }
}
