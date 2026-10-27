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
import org.lwjgl.opengl.GL11;

/** Native polish captures and pointer checks. Synthetic sources never touch music files. */
final class Release464PolishChecks {
  private static final Path REPORT = Path.of("power-beta-data/reports/release-464/polish");
  private static final int[][] SIZES = {{854, 480}, {320, 240}};
  private static final String CUSTOM = "polish464-building", EMPTY = "polish464-empty";
  private static final String NAME = "Quiet afternoons and late evening building sessions";
  private static final String FIRST = "music:calm1.ogg", SECOND = "music:calm2.ogg", THIRD = "music:creative1.ogg";
  private static final String SOURCE = "music/polish-check/Imported building music with a deliberately long folder name for warning wrapping";
  private static final String GROUP = MusicLibrarySources.id(SOURCE), SOURCE_NAME = "Imported building music";
  private static final int BLUE = PopupStyle.ACTIVE & 0xffffff;
  private static final List<String> captures = new ArrayList<>();
  private static final List<Map<String, Object>> observations = new ArrayList<>();

  private Release464PolishChecks() {}

  static void run(Minecraft mc, String action) throws Exception {
    check(Set.of("capture", "check").contains(action), "use release-464-polish capture|check");
    SilentAudioChecks.ready(mc);
    check(mc.world != null && mc.player != null && !mc.world.isRemote, "open a disposable singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable client");
    check(Display.isActive(), "focus the disposable client for pointer checks");
    check(AudioController.currentTrackId().isEmpty(), "start with idle disposable music");
    Files.createDirectories(REPORT.resolve("probes")); captures.clear(); observations.clear(); failures = 0;
    try (var sources = new SourceState(); var state = new Release462Checks.State(mc)) {
      // Every changed view exists before an acceptance assertion can fail.
      capture(mc, state);
      if (action.equals("check")) {
        test("RELEASE464 POLISH exact song/catalog hover padding and catalog controls", () -> rows(mc, state));
        test("RELEASE464 POLISH real pointer threshold, hover suppression, ghosts and blue drop lines", () -> drags(mc, state));
        test("RELEASE464 POLISH owned cancel, motion releases and committed order", () -> ownership(mc, state));
        test("RELEASE464 POLISH readonly titles and clean/dirty playlist footer", () -> titlesAndFooter(mc, state));
        test("RELEASE464 POLISH Library count, volume and folder removal bounds", () -> groups(mc, state));
        test("RELEASE464 POLISH centered wrapped native warning copy", () -> warnings(mc, state));
        test("RELEASE464 POLISH Options and Defaults warnings own hover and cancel input", () -> optionWarnings(mc, state));
        test("RELEASE464 POLISH bounded visible rows and native large-playlist render observations", () -> performance(mc, state));
      }
    } finally {
      Files.writeString(REPORT.resolve("captures.json"), Catalog.JSON.toJson(captures));
      Files.writeString(REPORT.resolve(action + ".json"), Catalog.JSON.toJson(Map.of("action", action,
          "failures", failures, "observations", observations,
          "limits", List.of("Synthetic folder ownership uses existing built-in tracks; no import or decoding test.",
              "Focus loss uses the shared drag update seam; physical desktop focus changes remain a manual check.",
              "Render timings describe this native fixture only; no before/after FPS claim."))));
    }
    log("RELEASE464 POLISH " + action + " failures=" + failures + " actualCaptures=" + captures.size());
    check(failures == 0, "release-464-polish failures=" + failures);
  }

  private static PowerOptionsScreen open(Release462Checks.State state, int[] size) throws Exception {
    if (state.options != null) { state.options.session().discard(); state.options.removed(); }
    var audio = new AudioSettings();
    audio.playlists.add(new MusicPlaylist(CUSTOM, NAME, List.of(FIRST, SECOND, THIRD)));
    audio.playlists.add(new MusicPlaylist(EMPTY, "Empty playlist", List.of()));
    audio.favourites.add(FIRST); audio.sounds.put(FIRST, 35);
    audio.musicDirectories.add(SOURCE); audio.libraryGroupVolumes.put(GROUP, 40);
    MusicPlaylist.load(audio, CUSTOM); MusicPlaylist.loadMenu(audio, MusicPlaylist.SAME_AS_WORLD);
    AudioConfig.preview(audio);
    set(AudioController.class, "sourceLibrary", new MusicLibrarySources.Snapshot(
        List.of(new MusicLibrarySources.Folder(GROUP, SOURCE, SOURCE_NAME, "Ready", List.of(FIRST, SECOND), List.of())),
        Map.of(FIRST, GROUP, SECOND, GROUP), List.of()));
    set(AudioController.class, "rotationConfig", AudioConfig.current());
    set(AudioController.class, "enabledTracks", List.of());
    MusicRequests.edit(queue -> queue.tracks.clear());
    var options = new PowerOptionsScreen(state.mc.currentScreen); state.options = options;
    state.mc.setScreen(options); options.init(state.mc, size[0], size[1]);
    set(options, "page", "Audio"); set(options, "libraryOpen", false); set(options, "allSounds", false);
    set(options, "conflictIds", List.of()); set(options, "changedOnly", false);
    ((TextInput)get(options, "search")).setText(""); ((Set<?>)get(options, "collapsed")).clear();
    call(options, "layout"); call(options, "editMusic");
    return options;
  }

  private static void capture(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) for (String view : List.of("catalog", "catalog-hover", "catalog-drag", "library-folders",
        "library-group-drag", "playlist-drag", "beta-title", "favorites-title", "playlist-clean", "playlist-dirty",
        "playlist-grouped", "warning-delete", "warning-folder", "warning-dirty")) {
      var options = open(state, size); var library = library(options); int mx = -1, my = -1;
      switch (view) {
        case "catalog", "catalog-hover", "catalog-drag", "warning-delete" -> {
          library.openPresets(); Object panel = panel(library), layout = call(panel, "catalogLayout");
          if (view.equals("catalog-hover")) { my = catalogY(library, CUSTOM) + 10; mx = n(layout, "duplicateX") + 8; }
          else if (view.equals("catalog-drag")) {
            int y = n(panel, "rowTop"); mx = n(layout, "nameX") + 5; my = Math.min(n(panel, "bottom") - 16, y + 40);
            press(options, mx, y + 10, 0);
          } else if (view.equals("warning-delete")) delete(options, library);
        }
        case "library-folders", "library-group-drag", "warning-folder" -> {
          library.showTracks(); expand(library); int y = groupY(library, GROUP);
          if (view.equals("library-group-drag")) {
            ((Set<String>)get(library, "collapsed")).add(GROUP); call(library, "rebuildRows");
            mx = n(library, "left") + 20; my = Math.min(n(library, "bottom") - 16, y + 38); press(options, mx, y + 8, 0);
          } else if (view.equals("warning-folder")) press(options, n(library, "sourceRemoveX") + 8, y + 8, 0);
        }
        case "beta-title" -> library.showPlaylist(MusicPlaylist.BETA);
        case "favorites-title" -> library.showPlaylist(MusicPlaylist.FAVORITES);
        default -> {
          library.showPlaylist(CUSTOM);
          if (view.equals("playlist-drag")) {
            int y = trackY(library, FIRST); mx = n(library, "left") + 12;
            my = Math.min(n(library, "bottom") - 16, y + 38); press(options, mx, y + 10, 0);
          } else if (view.equals("playlist-grouped")) grouped(library);
          else if (view.equals("playlist-dirty") || view.equals("warning-dirty")) {
            draft(library).include(SECOND, false); call(library, "syncPanel");
            if (view.equals("warning-dirty")) library.showTracks();
          }
        }
      }
      shot(mc, options, view, size, mx, my, true);
    }
    for (int[] size : SIZES) for (String kind : List.of("close", "reset", "defaults")) {
      var options = open(state, size); ModalView view = prepareOptionWarning(mc, options, kind, size);
      shot(mc, options, "warning-options-" + kind, size, view.buttonX + 8, view.buttonY + 8, true);
    }
  }

  private static void rows(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) {
      var options = open(state, size); var library = library(options); library.showPlaylist(CUSTOM);
      int left = n(library, "left"), right = n(library, "right"), y = trackY(library, FIRST);
      check(MusicRowLayout.hoverLeft(left) == left - 2 && MusicRowLayout.hoverRight(right) == right + 4,
          "shared hover padding differs from left - 2, right + 4");
      hoverMask(mc, options, "song", size, left, right, y, left + 5, y + 10);
      library.cancelPlaylist(); library.openPresets(); Object panel = panel(library), layout = call(panel, "catalogLayout");
      y = catalogY(library, CUSTOM); int cy = n(layout, "controlsY", new Class<?>[]{int.class}, y);
      check(n(layout, "nameX") == left + 8 && n(layout, "activeX") == left + 1, "catalog marker differs from Defaults text spacing");
      check(n(layout, "useX") + n(layout, "useWidth") + 4 == n(layout, "duplicateX")
          && n(layout, "duplicateX") + n(layout, "duplicateWidth") + 4 == n(layout, "deleteX"), "Use/Duplicate/Delete order or spacing changed");
      hoverMask(mc, options, "catalog", size, left, right, y, n(layout, "duplicateX") + 5, cy + 8);
      BufferedImage active = shot(mc, options, "catalog-active", size, -1, -1, false);
      for (int py = y + 7; py < y + 21; py++) for (int px = left; px < left + 4; px++) {
        boolean marker = px >= left + 1 && px < left + 3 && py >= y + 9 && py < y + 19;
        check((color(active, px, py) == 0xffdd66) == marker, "active playlist marker does not match Defaults 2 by 10 bounds");
      }
      int betaY = catalogY(library, MusicPlaylist.BETA), use = n(layout, "useX");
      cy = n(layout, "controlsY", new Class<?>[]{int.class}, betaY);
      press(options, use, cy + 8, 0);
      check(AudioConfig.current().playlist.equals(MusicPlaylist.BETA) && !library.playlistEditor(), "Use left endpoint missed or opened editor");
      List<String> before = ids(library);
      press(options, n(layout, "deleteX") + 19, cy + 8, 0);
      check(!library.modal() && ids(library).equals(before), "protected Delete accepted its right endpoint");
      int customY = catalogY(library, CUSTOM); cy = n(layout, "controlsY", new Class<?>[]{int.class}, customY);
      press(options, n(layout, "duplicateX") + n(layout, "duplicateWidth") - 1, cy + 8, 0);
      check(ids(library).size() == before.size() + 1, "Duplicate right endpoint missed drawn control");
      library.openPresets(); int first = n(panel, "rowTop");
      press(options, left - 2, first + 4, 0); release(options, left - 2, first + 4, 0);
      check(library.playlistEditor(), "left hover padding does not share catalog click bounds");
      library.cancelPlaylist(); library.openPresets();
      press(options, right + 3, first + 4, 0); release(options, right + 3, first + 4, 0);
      check(library.playlistEditor(), "right hover padding does not share catalog click bounds");
    }
  }

  private static void hoverMask(Minecraft mc, PowerOptionsScreen options, String label, int[] size,
      int left, int right, int y, int mx, int my) throws Exception {
    BufferedImage plain = shot(mc, options, label + "-plain", size, -1, -1, false);
    BufferedImage hover = shot(mc, options, label + "-hover", size, mx, my, false);
    for (int x = left - 3; x <= right + 4; x++) {
      int before = color(plain, x, y + 1), after = color(hover, x, y + 1);
      if (x < left - 2 || x >= right + 4) check(before == after, label + " hover escaped shared bounds");
      else for (int shift : new int[]{0, 8, 16})
        check(Math.abs((after >> shift & 255) - (before >> shift & 255) * 159d / 255) <= 1,
            label + " hover did not fill its entire shared padding at " + x);
    }
  }

  private static void drags(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) for (String kind : List.of("catalog", "playlist", "group")) {
      var options = open(state, size); var library = library(options); Object owner; int top, bottom, y, x; String label;
      if (kind.equals("catalog")) {
        library.openPresets(); owner = panel(library); top = n(owner, "rowTop"); bottom = n(owner, "bottom");
        y = top + 10; x = n(call(owner, "catalogLayout"), "nameX") + 5; label = playlists(library).get(0).name();
      } else {
        if (kind.equals("playlist")) library.showPlaylist(CUSTOM); else library.showTracks();
        owner = library; top = n(library, "listTop"); bottom = n(library, "bottom");
        y = top + 10; x = n(library, "left") + (kind.equals("group") ? 20 : 12);
        label = kind.equals("group") ? SOURCE_NAME : AudioController.musicLabel(FIRST);
      }
      Object drag = get(owner, "reorder");
      BufferedImage plain = shot(mc, options, kind + "-drag-plain", size, -1, -1, false);
      press(options, x, y, 0); BufferedImage jitter = shot(mc, options, kind + "-jitter", size, x + 3, y + 3, false);
      check((boolean)call(drag, "active") && !(boolean)call(drag, "dragging"), kind + " 3px jitter became a drag");
      if (!kind.equals("group")) {
        int px = MusicRowLayout.hoverLeft(n(library, "left"));
        for (int shift : new int[]{0, 8, 16})
          check(Math.abs((color(jitter, px, top + 1) >> shift & 255) - (color(plain, px, top + 1) >> shift & 255) * 159d / 255) <= 1,
              kind + " press suppressed hover before actual dragging");
      }
      release(options, x + 3, y + 3, -1);
      check((boolean)call(drag, "active"), kind + " native motion release lost ownership");
      int mx = x + 8, my = y;
      BufferedImage moving = shot(mc, options, kind + "-threshold", size, mx, my, false);
      check((boolean)call(drag, "dragging"), kind + " exact 8px move did not become a drag");
      // The row background and all controls under the pointer stay in their ordinary state.
      int left = MusicRowLayout.hoverLeft(n(library, "left")), right = MusicRowLayout.hoverRight(n(library, "right"));
      int sampleY = top + 3;
      for (int px = left; px < right; px++) if (color(moving, px, sampleY) != BLUE)
        check(color(moving, px, sampleY) == color(plain, px, sampleY), kind + " drag retained row hover");
      blueLine(moving, left, right, top, bottom, kind);
      ghost(mc, moving, label, size, mx, my, kind);
      int controlX, controlY, controlWidth;
      if (kind.equals("catalog")) {
        Object layout = call(owner, "catalogLayout");
        controlX = n(layout, "duplicateX"); controlWidth = n(layout, "duplicateWidth");
        controlY = n(layout, "controlsY", new Class<?>[]{int.class}, top);
      } else if (kind.equals("group")) {
        controlX = n(library, "sourceRemoveX"); controlWidth = 18; controlY = top + 1;
      } else {
        MusicRowLayout layout = (MusicRowLayout)call(library, "rowLayout");
        controlX = layout.favouriteX(); controlWidth = layout.buttonWidth(); controlY = top + layout.controlsY();
      }
      BufferedImage controls = shot(mc, options, kind + "-drag-controls", size, controlX + 5, controlY + 8, false);
      // A clamped two-pixel insertion line may cross the folder X's top border.
      // Its interior still proves that the control did not acquire hover styling.
      for (int py = controlY + 2; py < controlY + 10; py++) for (int px = controlX + 2; px < controlX + controlWidth - 2; px++)
        check(color(controls, px, py) == color(plain, px, py), kind + " drag retained control hover");
      mx += 20; my = Math.min(bottom - 16, my + 15);
      BufferedImage moved = shot(mc, options, kind + "-ghost-follow", size, mx, my, false);
      ghost(mc, moved, label, size, mx, my, kind);
      check(library.hoverHelp().isEmpty(), kind + " drag retained a row tooltip");
      key(options, Keyboard.KEY_ESCAPE); release(options, mx, my, 0);
      check(!(boolean)call(drag, "active"), kind + " Escape left pointer ownership");
      observations.add(Map.of("case", "native-drag", "kind", kind, "width", size[0], "jitterPixels", 3, "thresholdPixels", 8));
    }
  }

  private static void blueLine(BufferedImage image, int left, int right, int top, int bottom, String kind) {
    int lines = 0, first = -1, last = -1;
    for (int y = top; y < bottom; y++) if (color(image, left, y) == BLUE && color(image, right - 1, y) == BLUE) {
      if (first < 0) first = y; last = y;
      lines++; for (int x = left; x < right; x++) check(color(image, x, y) == BLUE, kind + " drop line has a gap");
    }
    check(lines == 2 && last == first + 1, kind + " drop line is not exactly two adjacent blue pixels high");
  }

  private static void ghost(Minecraft mc, BufferedImage image, String label, int[] size, int mx, int my, String kind) throws Exception {
    var probe = new TextProbe(); probe.init(mc, size[0], size[1]);
    String fitted = (String)call(probe, "fit", new Class<?>[]{String.class, int.class}, label, Math.min(320, size[0] - 24));
    int width = mc.textRenderer.getWidth(fitted) + 16;
    int x = Math.max(4, Math.min(size[0] - width - 4, mx + 12)), y = Math.max(4, Math.min(size[1] - 22, my + 12));
    for (int py = y; py < y + 18; py++) for (int px = x; px < x + 2; px++)
      check(color(image, px, py) == BLUE, kind + " ghost marker did not follow pointer");
    var text = new TextProbe(fitted, x + 8, y + 5, 0xffffff); text.init(mc, size[0], size[1]);
    BufferedImage reference = shot(mc, text, "reference-ghost-" + kind, size, -1, -1, false);
    textMask(image, reference, x + 8, y + 5, width - 16, 8, 0xffffff, kind + " ghost name");
  }

  private static void ownership(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) {
      var options = open(state, size); var library = library(options); library.showPlaylist(CUSTOM);
      int x = n(library, "left") + 12, y = trackY(library, FIRST) + 10;
      Object drag = get(library, "reorder"); List<String> before = draft(library).tracks();
      press(options, x, y, 0); release(options, x + 3, y + 3, 0);
      check(draft(library).tracks().equals(before) && !library.playlistDirty(), "song jitter altered draft");
      press(options, x, y, 0); call(drag, "update", new Class<?>[]{int.class, int.class, boolean.class}, x + 12, y, false);
      release(options, x, y + 24, 0);
      check(!(boolean)call(drag, "active") && draft(library).tracks().equals(before), "lost-focus seam retained or committed song drag");
      press(options, x, y, 0); library.focus(); release(options, x, y + 24, 0);
      check(!(boolean)call(drag, "active") && draft(library).tracks().equals(before), "search focus stole an owned drag without canceling it");
      library.unfocus(); press(options, x, y, 0);
      int target = Math.min(n(library, "bottom") - 1, y + 30);
      shot(mc, options, "owned-release", size, x + 8, target, false); release(options, x + 8, target, 0);
      check(draft(library).tracks().get(1).equals(FIRST) && library.playlistDirty(), "song release failed to reorder its owned draft");
      check(MusicPlaylist.find(AudioConfig.current(), CUSTOM).tracks().equals(before), "song drag escaped the isolated playlist draft");
      library.cancelPlaylist(); library.showTracks();
      if (!library.sectionsCollapsed()) library.toggleSections();
      int heading = groupY(library, GROUP), groupX = n(library, "left") + 20;
      List<?> headings = (List<?>)get(library, "rows");
      check(headings.size() >= 2 && GROUP.equals(call(headings.get(0), "group")), "group reorder fixture lost its first heading");
      Object next = headings.get(1);
      int groupTarget = n(library, "listTop") + n(next, "y") + n(next, "height") / 2 + 1 - n(library, "scroll");
      check(groupTarget < n(library, "bottom"), "next group midpoint is outside the native viewport");
      press(options, groupX, heading + 8, 0);
      shot(mc, options, "group-owned-release", size, groupX, groupTarget, false);
      release(options, groupX, groupTarget, 0);
      check(AudioConfig.current().libraryOrder.indexOf(GROUP) == 1, "group release failed to persist heading order");
      check(MusicPlaylist.find(AudioConfig.current(), CUSTOM).tracks().equals(before), "group reorder changed saved membership");
      library.openPresets(); Object panel = panel(library), layout = call(panel, "catalogLayout");
      int top = n(panel, "rowTop"), height = n(panel, "rowHeight"); x = n(layout, "nameX") + 4;
      List<String> order = ids(library); press(options, x, top + 10, 0);
      shot(mc, options, "catalog-release", size, x + 8, Math.min(n(panel, "bottom") - 1, top + height + height / 2 + 1), false);
      // The small catalog can expose only one row; cancellation is checked there, persistence at large size.
      if (size[0] == 854) {
        release(options, x + 8, top + height + height / 2 + 1, 0);
        check(ids(library).get(1).equals(order.get(0)) && AudioConfig.current().playlistOrder.equals(ids(library)), "catalog release failed to persist order");
      } else { key(options, Keyboard.KEY_ESCAPE); release(options, x + 8, top + 20, 0); check(ids(library).equals(order), "small catalog Escape persisted order"); }
      library.openPresets(); order = ids(library); press(options, x, n(panel, "rowTop") + 10, 0);
      library.focus(); release(options, x + 12, n(panel, "rowTop") + 20, 0);
      check(ids(library).equals(order) && !library.playlistEditor(), "catalog search focus committed or opened the prior press");
    }
  }

  private static void titlesAndFooter(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) {
      for (String id : List.of(MusicPlaylist.BETA, MusicPlaylist.FAVORITES)) {
        var options = open(state, size); var library = library(options); library.showPlaylist(id); Object panel = panel(library);
        int x = n(panel, "nameX"), y = n(panel, "top"), width = n(panel, "nameWidth");
        BufferedImage actual = shot(mc, options, "readonly-" + id, size, -1, -1, false);
        for (int px = x - 1; px < x + width + 1; px++) check(color(actual, px, y + 18) == 0x555555, "readonly title lacks complete TextInput underline");
        String title = (String)call(panel, "fit", new Class<?>[]{String.class, int.class}, draft(library).name(), width - 8);
        var probe = new TextProbe(title, x + 4, y + 5, 0xffffff); probe.init(mc, size[0], size[1]);
        BufferedImage reference = shot(mc, probe, "reference-title-" + id, size, -1, -1, false);
        textMask(actual, reference, x + 4, y + 5, mc.textRenderer.getWidth(title), 8, 0xffffff, "readonly title");
        press(options, x + 6, y + 8, 0); key(options, Keyboard.KEY_A);
        check(!((TextInput)get(panel, "name")).focused && draft(library).name().equals(MusicPlaylist.find(AudioConfig.current(), id).name()), "readonly title accepted editing");
      }
      var options = open(state, size); var library = library(options); library.showPlaylist(CUSTOM);
      List<String> saved = MusicPlaylist.find(AudioConfig.current(), CUSTOM).tracks();
      check(footerLabels(options).equals(List.of("Save playlist")), "clean playlist retained Cancel");
      check(!(boolean)call(footer(options).get(0), "enabled"), "clean playlist enabled Save");
      draft(library).include(SECOND, false); call(library, "syncPanel");
      check(footerLabels(options).equals(List.of("Cancel", "Save playlist")), "dirty playlist lost Cancel or Save");
      Object cancel = footer(options).get(0);
      press(options, n(cancel, "x") + 5, size[1] - 20, 0);
      check(!library.playlistDirty() && MusicPlaylist.find(AudioConfig.current(), CUSTOM).tracks().equals(saved), "Cancel published the dirty draft");
      if (library.playlistEditor()) check(draft(library).tracks().equals(saved), "Cancel reopened an altered active playlist draft");
    }
  }

  private static void groups(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) {
      var options = open(state, size); var library = library(options); library.showTracks(); expand(library);
      int y = groupY(library, GROUP), x = n(library, "groupVolumeX"), width = n(library, "groupVolumeWidth"), remove = n(library, "sourceRemoveX");
      int countRight = n(library, "countRight", new Class<?>[]{String.class}, GROUP);
      MusicRowLayout track = (MusicRowLayout)call(library, "rowLayout");
      check(x == track.volumeX() && x + width + 4 == remove && remove + 18 == n(library, "right"), "Library group slider does not share song left edge and four-pixel delete gap");
      check(countRight == x - 8 && n(library, "countRight", new Class<?>[]{String.class}, "Alpha") == countRight,
          "Library source and included group count columns differ");
      BufferedImage actual = shot(mc, options, "group-controls", size, -1, -1, false);
      String count = "2 tracks"; int countWidth = mc.textRenderer.getWidth(count);
      var probe = new TextProbe(count, countRight - countWidth, y + 6, 0xaaaaaa); probe.init(mc, size[0], size[1]);
      BufferedImage reference = shot(mc, probe, "reference-group-count", size, -1, -1, false);
      textMask(actual, reference, countRight - countWidth, y + 6, countWidth, 8, 0xaaaaaa, "Library count");
      int longestCountX = countRight - mc.textRenderer.getWidth(MusicLibrary.MAX_TRACKS + " tracks"), left = n(library, "left");
      String longestHeading = "v " + call(library, "fit", new Class<?>[]{String.class, int.class}, SOURCE_NAME, Math.max(12, longestCountX - 6 - left - 12));
      check(left + 2 + mc.textRenderer.getWidth(longestHeading) <= longestCountX - 6, "compact group title overlaps the maximum source track count");
      for (int edge : new int[]{x, x + width - 1}) {
        press(options, edge, y + 8, 0); release(options, edge, y + 8, 0);
        check(AudioConfig.current().libraryGroupVolumes.getOrDefault(GROUP, 100) == (edge == x ? 0 : 100), "group slider endpoint missed drawn bounds");
      }
      press(options, x + width - 1, y + 8, 1); Object editor = get(options, "valueEditor");
      check((boolean)call(editor, "active") && number(editor, "x") == x && number(editor, "width") == width, "group exact entry does not match slider");
      key(options, Keyboard.KEY_ESCAPE); press(options, remove + 17, y + 8, 0);
      check(library.modal(), "folder X right endpoint missed"); key(options, Keyboard.KEY_ESCAPE);
      check(!library.modal() && AudioConfig.current().musicDirectories.contains(SOURCE), "folder warning Escape removed source");
      library.showPlaylist(CUSTOM); grouped(library);
      check(!((List<?>)get(library, "rows")).isEmpty(), "grouped playlist lost rows");
      check(!(boolean)call(library, "groupHasVolume", new Class<?>[]{String.class}, GROUP), "Library group gain leaked into playlist view");
      check(n(library, "countRight", new Class<?>[]{String.class}, GROUP) == n(library, "right") - 4, "grouped playlist count lost its right edge");
      int groupedY = groupY(library, GROUP), groupedX = n(library, "right") - 4 - mc.textRenderer.getWidth("2/2");
      BufferedImage grouped = shot(mc, options, "grouped-membership-count", size, -1, -1, false);
      var groupedProbe = new TextProbe("2/2", groupedX, groupedY + 6, 0xaaaaaa); groupedProbe.init(mc, size[0], size[1]);
      textMask(grouped, shot(mc, groupedProbe, "reference-grouped-membership-count", size, -1, -1, false),
          groupedX, groupedY + 6, mc.textRenderer.getWidth("2/2"), 8, 0xaaaaaa, "grouped playlist X/Y count");
    }
  }

  private static void warnings(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) for (String kind : List.of("delete", "folder", "dirty")) {
      var options = open(state, size); var library = library(options); String title, message; int bottom;
      if (kind.equals("delete")) {
        library.openPresets(); delete(options, library); title = "Delete this playlist?";
        message = "The world playlist will return to Beta 1.7.3."; bottom = size[1] / 2 - 11;
      } else if (kind.equals("folder")) {
        library.showTracks(); expand(library); press(options, n(library, "sourceRemoveX") + 8, groupY(library, GROUP) + 8, 0);
        title = "Remove \"" + SOURCE_NAME + "\"?"; message = "Files and playlist entries stay unchanged.\n" + SOURCE; bottom = size[1] / 2 + 1;
      } else {
        library.showPlaylist(CUSTOM); draft(library).include(SECOND, false); call(library, "syncPanel"); library.showTracks();
        title = "Save changes to this playlist?"; message = ""; bottom = size[1] / 2 - 11;
      }
      check(library.modal(), kind + " warning did not open");
      BufferedImage actual = shot(mc, options, "warning-mask-" + kind, size, -1, -1, false);
      Class<?> warning = Class.forName("local.luke.power.ui.WarningText");
      @SuppressWarnings("unchecked") List<Object> lines = (List<Object>)call(warning, "layout",
          new Class<?>[]{int.class, int.class, String.class, String.class, int.class, ToIntFunction.class},
          Math.min(312, size[0] - 24), bottom, title, message, 0xaaaaaa, (ToIntFunction<String>)mc.textRenderer::getWidth);
      int last = -1;
      for (int i = 0; i < lines.size(); i++) {
        Object line = lines.get(i); String text = (String)call(line, "text"); int y = n(line, "y"), color = n(line, "color");
        int width = mc.textRenderer.getWidth(text), x = size[0] / 2 - width / 2;
        check(x >= 12 && x + width <= size[0] - 12 && y >= 12 && y > last && y + 8 <= bottom, kind + " warning escapes bounds"); last = y;
        var probe = new TextProbe(text, x, y, color); probe.init(mc, size[0], size[1]);
        BufferedImage reference = shot(mc, probe, "reference-warning-" + kind + "-" + i, size, -1, -1, false);
        textMask(actual, reference, 12, y, size[0] - 24, 8, color, kind + " centered warning");
      }
      if (kind.equals("folder") && size[0] == 320) check(lines.size() >= 4, "small folder warning did not wrap");
      if (kind.equals("folder")) {
        Object folder = get(library, "folders");
        buttonHover(mc, options, actual, "warning-" + kind, "Remove", size,
            n(folder, "dialogLeft"), n(folder, "buttonY"), n(folder, "buttonWidth"));
      } else {
        Object panel = panel(library);
        buttonHover(mc, options, actual, "warning-" + kind, kind.equals("dirty") ? "Save" : "Delete", size,
            n(panel, "dialogButtonX", new Class<?>[]{int.class}, 0), n(panel, "dialogButtonY"), n(panel, "dialogButtonWidth"));
      }
      key(options, Keyboard.KEY_ESCAPE); check(!library.modal(), kind + " warning Escape retained modal");
    }
  }

  private record ModalView(String kind, String label, int buttonX, int buttonY, int cancelX, int cancelY,
      int underlayX, int underlayY, int underlayWidth, int underlayHeight) {}

  private static ModalView prepareOptionWarning(Minecraft mc, PowerOptionsScreen options, String kind, int[] size) throws Exception {
    set(options, "libraryOpen", false); set(options, "page", kind.equals("defaults") ? "Defaults" : "Video");
    ((TextInput)get(options, "search")).setText(""); set(options, "scroll", 0d); call(options, "layout");
    int ux, uy, uw, uh;
    if (kind.equals("defaults")) {
      Object panel = get(options, "defaultsPanel");
      ((UiScreen)panel).init(mc, size[0], size[1]);
      int left = n(options, "left"), span = n(options, "span"), rowSpan = n(panel, "rowSpan", new Class<?>[]{int.class}, span);
      int natural = n(panel, "rowTop", new Class<?>[]{int.class, int.class}, span, 0);
      Object track = call(panel, "track", new Class<?>[]{int.class, int.class}, left, span);
      set(panel, "scroll", (double)Math.min(n(track, "maximum"), Math.max(0, natural - 72)));
      int top = n(panel, "rowTop", new Class<?>[]{int.class, int.class}, span, 0);
      ux = n(panel, "loadX", new Class<?>[]{int.class, int.class}, left, rowSpan);
      uy = n(panel, "buttonY", new Class<?>[]{int.class, int.class}, rowSpan, top);
      uw = n(panel, "buttonWidth", new Class<?>[]{int.class}, rowSpan); uh = 18;
      press(options, ux + 6, uy + 7, 0);
      check((boolean)call(panel, "modal"), "Defaults Load did not open confirmation");
    } else {
      Object row = ((List<?>)get(options, "rows")).stream().filter(value -> {
        try { return call(value, "setting") != null; } catch (Exception failure) { throw new RuntimeException(failure); }
      }).findFirst().orElseThrow();
      ux = n(options, "left"); uy = n(options, "top") + n(row, "y"); uw = n(options, "span"); uh = n(row, "height");
      if (kind.equals("close")) {
        Setting master = options.session().settings().stream().filter(value -> value.id.equals("audio.master")).findFirst().orElseThrow();
        master.value = new com.google.gson.JsonPrimitive(80); call(options, "changed", new Class<?>[]{Setting.class}, master);
        press(options, n(options, "actionX", new Class<?>[]{int.class}, 0) + 6, size[1] - 20, 0);
      } else press(options, n(options, "resetX") + 6, n(options, "footerY") + 8, 0);
      check((boolean)get(options, kind.equals("close") ? "confirmClose" : "confirmReset"), "Options " + kind + " did not open confirmation");
    }
    int buttonY = size[1] / 2 + (kind.equals("defaults") ? 24 : 12);
    return new ModalView(kind, kind.equals("defaults") ? "Load" : kind.equals("close") ? "Save" : "Reset",
        size[0] / 2 - 102, buttonY, size[0] / 2 + (kind.equals("close") ? -50 : 2),
        buttonY + (kind.equals("close") ? 24 : 0), ux, uy, uw, uh);
  }

  private static void optionWarnings(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) for (String kind : List.of("close", "reset", "defaults")) {
      var options = open(state, size); ModalView view = prepareOptionWarning(mc, options, kind, size);
      DefaultBases.Base base = DefaultBases.current(); long changes = options.session().changes();
      BufferedImage plain = shot(mc, options, "options-" + kind + "-plain", size, -1, -1, false);
      int mx = view.underlayX + 6, my = view.underlayY + 7;
      check(my < view.buttonY || my >= view.cancelY + 20, "underlay probe overlaps a warning button");
      BufferedImage hovered = shot(mc, options, "options-" + kind + "-underlay", size, mx, my, false);
      samePixels(plain, hovered, 0, 0, size[0], size[1], "Options " + kind + " warning leaked pointer hover underneath");
      buttonHover(mc, options, plain, "options-" + kind, view.label, size, view.buttonX, view.buttonY, 100);
      press(options, view.cancelX + 8, view.cancelY + 8, 0);
      check(mc.currentScreen == options && options.session().changes() == changes && DefaultBases.current() == base,
          "warning Cancel changed or closed its settings session");
      check(kind.equals("defaults") ? !(boolean)call(get(options, "defaultsPanel"), "modal")
          : !(boolean)get(options, kind.equals("close") ? "confirmClose" : "confirmReset"), "warning Cancel did not close modal");
      // The chosen underlying row really reacts to this pointer once the warning is gone.
      BufferedImage ordinary = shot(mc, options, "options-" + kind + "-ordinary", size, -1, -1, false);
      BufferedImage ordinaryHover = shot(mc, options, "options-" + kind + "-ordinary-hover", size, mx, my, false);
      check(changedPixels(ordinary, ordinaryHover, view.underlayX, view.underlayY,
          view.underlayWidth, view.underlayHeight) > 0, "underlay probe selected a row with no hover response");
      observations.add(Map.of("case", "modal-pointer-ownership", "kind", kind, "width", size[0], "underlayChangedPixels", 0));
    }
  }

  private static void buttonHover(Minecraft mc, PowerOptionsScreen options, BufferedImage plain, String name,
      String label, int[] size, int x, int y, int width) throws Exception {
    BufferedImage hovered = shot(mc, options, name + "-button-hover", size, x + 8, y + 8, false);
    int textX = x + width / 2 - mc.textRenderer.getWidth(label) / 2;
    var probe = new TextProbe(label, textX, y + 6, 0xffffa0); probe.init(mc, size[0], size[1]);
    BufferedImage reference = shot(mc, probe, "reference-" + name + "-button", size, -1, -1, false);
    textMask(hovered, reference, x, y + 6, width, 8, 0xffffa0, name + " warning button hover");
    check(changedPixels(plain, hovered, x, y, width, 20) > 0, name + " warning button ignored actual pointer");
  }

  private static int changedPixels(BufferedImage first, BufferedImage second, int x, int y, int width, int height) {
    int changed = 0;
    for (int py = y; py < y + height; py++) for (int px = x; px < x + width; px++)
      if (color(first, px, py) != color(second, px, py)) changed++;
    return changed;
  }

  private static void samePixels(BufferedImage first, BufferedImage second, int x, int y, int width, int height, String message) {
    check(changedPixels(first, second, x, y, width, height) == 0, message);
  }

  private static void performance(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) {
      var options = open(state, size); var library = library(options);
      var ids = new ArrayList<String>(); for (int i = 0; i < 4096; i++) ids.add("music:polish-check/absent-" + i + ".ogg");
      Object edits = get(library, "edits"); call(edits, "save", new Class<?>[]{MusicPlaylist.class}, new MusicPlaylist("polish464-large", "Large native playlist", ids));
      library.showPlaylist("polish464-large"); int rowHeight = n(library, "rowHeight"), viewport = n(library, "bottom") - n(library, "listTop");
      Object index = get(library, "rowIndex"); check(((List<?>)get(library, "rows")).size() == 4096, "large playlist fixture truncated rows");
      int maximum = 0;
      for (int position : new int[]{0, rowHeight * 2048 + 3, rowHeight * 4096 - viewport}) {
        call(library, "scroll", new Class<?>[]{int.class}, position); int scroll = n(library, "scroll");
        int first = n(index, "firstVisible", new Class<?>[]{int.class}, scroll), limit = n(index, "limitVisible", new Class<?>[]{int.class}, scroll + viewport);
        check(first >= 0 && limit <= 4096 && limit - first <= (viewport + rowHeight - 1) / rowHeight + 1, "visible row query grows with playlist size");
        check(n(index, "at", new Class<?>[]{int.class}, scroll + 1) == (scroll + 1) / rowHeight, "indexed pointer row differs at deep scroll");
        maximum = Math.max(maximum, limit - first);
      }
      shot(mc, options, "large-playlist", size, -1, -1, false);
      long[] samples = new long[21];
      int oldWidth = mc.displayWidth, oldHeight = mc.displayHeight;
      mc.displayWidth = size[0] * 2; mc.displayHeight = size[1] * 2;
      try {
        for (int i = 0; i < 5; i++) { options.render(-1, -1, 0); GL11.glFinish(); }
        for (int i = 0; i < samples.length; i++) { long start = System.nanoTime(); options.render(-1, -1, 0); GL11.glFinish(); samples[i] = System.nanoTime() - start; }
      } finally { mc.displayWidth = oldWidth; mc.displayHeight = oldHeight; }
      Arrays.sort(samples);
      check(get(library, "rowIndex") == index, "unchanged render rebuilt row index");
      observations.add(Map.of("case", "native-large-playlist-render", "width", size[0], "rows", ids.size(), "maximumVisibleRows", maximum,
          "medianMicros", samples[samples.length / 2] / 1000d, "samples", samples.length,
          "scope", "Options render plus glFinish, no readback or PNG encoding; no comparative FPS claim"));
    }
  }

  private static void delete(PowerOptionsScreen options, MusicLibraryScreen library) throws Exception {
    Object layout = call(panel(library), "catalogLayout"); int y = catalogY(library, CUSTOM);
    press(options, n(layout, "deleteX") + 8, n(layout, "controlsY", new Class<?>[]{int.class}, y) + 8, 0);
  }
  private static MusicLibraryScreen library(PowerOptionsScreen options) throws Exception { return (MusicLibraryScreen)get(options, "library"); }
  private static Object panel(MusicLibraryScreen library) throws Exception { return get(library, "presets"); }
  private static MusicPresetDraft draft(MusicLibraryScreen library) throws Exception { return (MusicPresetDraft)call(panel(library), "draft"); }
  @SuppressWarnings("unchecked") private static List<MusicPlaylist> playlists(MusicLibraryScreen library) throws Exception { return (List<MusicPlaylist>)call(panel(library), "filteredPresets"); }
  private static List<String> ids(MusicLibraryScreen library) throws Exception { return playlists(library).stream().map(MusicPlaylist::id).toList(); }
  private static List<?> footer(PowerOptionsScreen options) throws Exception { return (List<?>)call(options, "draftFooter"); }
  private static List<String> footerLabels(PowerOptionsScreen options) throws Exception { var result = new ArrayList<String>(); for (Object button : footer(options)) result.add((String)call(button, "label")); return result; }
  private static void expand(MusicLibraryScreen library) throws Exception { ((Set<?>)get(library, "collapsed")).clear(); call(library, "rebuildRows"); }
  private static void grouped(MusicLibraryScreen library) throws Exception { set(panel(library), "grouped", true); call(library, "syncPanel"); expand(library); }
  private static int catalogY(MusicLibraryScreen library, String id) throws Exception {
    Object panel = panel(library); int index = ids(library).indexOf(id); check(index >= 0, "missing catalog fixture " + id);
    int height = n(panel, "rowHeight"), top = n(panel, "rowTop"), bottom = n(panel, "bottom");
    int scroll = Math.max(0, index * height - Math.max(0, bottom - top - height)); set(panel, "scroll", scroll);
    return top + index * height - scroll;
  }
  private static int trackY(MusicLibraryScreen library, String id) throws Exception { return reveal(library, id, false); }
  private static int groupY(MusicLibraryScreen library, String group) throws Exception { return reveal(library, group, true); }
  private static int reveal(MusicLibraryScreen library, String value, boolean group) throws Exception {
    for (Object row : (List<?>)get(library, "rows")) if (group ? call(row, "id") == null && value.equals(call(row, "group")) : value.equals(call(row, "id"))) {
      call(library, "scroll", new Class<?>[]{int.class}, Math.max(0, n(row, "y") - (group ? 0 : 20)));
      return n(library, "listTop") + n(row, "y") - n(library, "scroll");
    }
    throw new AssertionError("missing native row " + value);
  }
  private static void press(Screen screen, int x, int y, int button) { ((ScreenInput)(Object)screen).power$click(x, y, button); }
  private static void release(Screen screen, int x, int y, int button) { ((ScreenInput)(Object)screen).power$release(x, y, button); }
  private static void key(Screen screen, int key) { ((ScreenInput)(Object)screen).power$key('\0', key); }
  private static BufferedImage shot(Minecraft mc, Screen screen, String label, int[] size, int x, int y, boolean actual) throws Exception {
    String name = label + "-" + size[0] + ".png"; Path file = (actual ? REPORT : REPORT.resolve("probes")).resolve(name);
    SettingsSnapshot.render(mc, screen, size[0], size[1], file, x, y);
    if (actual) captures.add(name); return ImageIO.read(file.toFile());
  }
  private static int color(BufferedImage image, int x, int y) { return image.getRGB(x * 2, y * 2) & 0xffffff; }
  private static void textMask(BufferedImage actual, BufferedImage reference, int x, int y, int width, int height, int color, String label) {
    int ink = 0;
    for (int py = y; py < y + height; py++) for (int px = x; px < x + width; px++) {
      boolean expected = color(reference, px, py) == color; if (expected) ink++;
      check((color(actual, px, py) == color) == expected, label + " native font mask differs");
    }
    check(ink > 0, label + " reference contains no font pixels");
  }
  private static final class TextProbe extends UiScreen {
    final String label; final int x, y, color;
    TextProbe() { this("", 0, 0, 0xffffff); }
    TextProbe(String label, int x, int y, int color) { this.label = label; this.x = x; this.y = y; this.color = color; }
    @Override public void render(int mx, int my, float delta) { fill(0, 0, width, height, 0xff000000); text(label, x, y, color); }
  }
  private static int number(Object owner, String name) throws Exception { return ((Number)get(owner, name)).intValue(); }
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
