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
import net.minecraft.class_525;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

/** Native volume, popup and transport geometry; reference probes are kept outside the UI manifest. */
final class Release464SharedUiChecks {
  private static final int[][] SIZES = {{854, 480}, {320, 240}};
  private static final Path REPORT = Path.of("power-beta-data/reports/release-464/shared-ui");
  private static final String LONG = "Long playlist title for checking full names in the playlist selection tooltip";
  private static final List<String> captures = new ArrayList<>();
  private static final List<Map<String, Object>> observations = new ArrayList<>();

  private Release464SharedUiChecks() {}

  static void run(Minecraft mc, String action) throws Exception {
    check(action.equals("capture") || action.equals("check"), "use release-464-shared-ui capture|check");
    SilentAudioChecks.ready(mc);
    check(Display.isActive(), "focus the disposable client before native popup checks");
    check(mc.world != null && !mc.world.isRemote && mc.options.musicVolume == 0 && mc.options.soundVolume == 0,
        "use a muted disposable singleplayer world");
    check(AudioController.currentTrackId().isEmpty(), "start with idle disposable music");
    Files.createDirectories(REPORT); captures.clear(); observations.clear(); failures = 0;
    try (var state = new Release462Checks.State(mc)) {
      // Capture first, so an acceptance failure cannot hide the changed native views.
      capture(mc, state);
      if (action.equals("check")) {
        test("RELEASE464 SHARED standard volume columns and contextual category inset", () -> volumes(mc, state));
        test("RELEASE464 SHARED exact entry and endpoint drag share the drawn control bounds", () -> inputs(mc, state));
        test("RELEASE464 SHARED native Music/All game sounds titles retain All breadcrumb metadata", () -> titles(mc, state));
        test("RELEASE464 SHARED playlist names have delayed tooltips only when truncated", () -> tooltips(mc, state));
        test("RELEASE464 SHARED settings popup masks unrelated row pointer hover", () -> modal(mc, state));
        test("RELEASE464 SHARED pause glyph uses a centered eight-pixel canvas in native controls", () -> glyphs(mc, state));
      }
    } finally {
      Files.writeString(REPORT.resolve("captures.json"), Catalog.JSON.toJson(captures));
      Files.writeString(REPORT.resolve(action + ".json"), Catalog.JSON.toJson(Map.of(
          "action", action, "failures", failures, "observations", observations)));
    }
    log("RELEASE464 SHARED " + action + " failures=" + failures + " actualCaptures=" + captures.size());
    check(failures == 0, "release-464-shared-ui failures=" + failures);
  }

  private static PowerOptionsScreen open(Release462Checks.State state, int[] size) throws Exception {
    if (state.options != null) { state.options.session().discard(); state.options.removed(); }
    var settings = new AudioSettings();
    settings.playlists.add(new MusicPlaylist("shared464-long", LONG, List.of("music:calm1.ogg")));
    settings.playlist = "shared464-long";
    AudioConfig.preview(settings);
    var options = new PowerOptionsScreen(state.mc.currentScreen); state.options = options;
    state.mc.setScreen(options); options.init(state.mc, size[0], size[1]);
    set(options, "page", "Audio"); set(options, "libraryOpen", false); set(options, "allSounds", false);
    set(options, "conflictIds", List.of()); set(options, "changedOnly", false);
    ((TextInput)get(options, "search")).setText(""); ((Set<?>)get(options, "collapsed")).clear();
    call(options, "layout");
    return options;
  }

  private static void capture(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) {
      var options = open(state, size); shot(mc, options, "audio-volume", size, -1, -1, true);
      show(options, "audio.category.blocks"); shot(mc, options, "audio-categories", size, -1, -1, true);
      query(options, "Game sounds"); shot(mc, options, "audio-category-search", size, -1, -1, true);
      options = open(state, size); call(options, "openAllSounds");
      shot(mc, options, "all-game-sounds", size, -1, -1, true);
      Object row = firstVolumeRow(options, "audio.sound."); reveal(options, row);
      int x = controlLeft(options, row), y = controlY(options, row);
      click(options, x + 3, y + 8, 1);
      shot(mc, options, "all-game-sounds-exact", size, -1, -1, true); key(options, Keyboard.KEY_ESCAPE);
      options = open(state, size);
      call(options, "openAllSounds");
      query(options, setting(options, "power_environment:config.MUSIC_CONFIG.volumeRainAmbient").label);
      shot(mc, options, "extra-ambient-volume", size, -1, -1, true);
      options = open(state, size); call(options, "editMusic");
      shot(mc, options, "music", size, -1, -1, true);
      openPicker(options); Object popup = get(options, "choices"); int index = labels(popup).indexOf(LONG);
      int px = number(popup, "x") + 12, py = popupY(popup, index) + 8;
      shot(mc, options, "playlist-tooltip", size, px, py, true);
    }
    transport(mc, state, false);
  }

  private static void volumes(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) {
      var options = open(state, size);
      int end = number(options, "right") - 38;
      for (String id : List.of("audio.master", "native.music", "native.sound")) {
        Object row = row(options, id);
        check(end - controlLeft(options, row) == VolumeControls.WIDTH, "primary volume lost its full 150 pixels: " + id);
        if (id.startsWith("native.")) auxiliary(options, row, true);
      }
      for (Setting setting : options.session().settings()) if (setting.id.startsWith("audio.category.")) {
        Object row = row(options, setting.id);
        check(controlLeft(options, row) == end - 150 + 16, "normal Audio category lost its 16-pixel inset");
        check(end - controlLeft(options, row) == 134, "category inset changed its right edge");
      }
      query(options, "Game sounds");
      int categories = 0;
      for (Object row : rows(options)) {
        Setting setting = (Setting)call(row, "setting");
        if (setting != null && setting.id.startsWith("audio.category.")) {
          categories++; check(end - controlLeft(options, row) == 150, "search retained category indentation");
        }
      }
      check(categories > 0, "category search fixture matched no categories");
      options = open(state, size); call(options, "openAllSounds"); int sounds = 0;
      for (Object row : rows(options)) {
        Setting setting = (Setting)call(row, "setting");
        if (setting == null || !VolumeControls.accepts(setting)) continue;
        sounds++; check(end - controlLeft(options, row) == 150, "All game sounds preview shortened " + setting.id);
        if ((boolean)call(options, "soundPreview", new Class<?>[] {Setting.class}, setting)) auxiliary(options, row, false);
      }
      check(sounds > 20, "All game sounds fixture did not cover individual sounds");
      options = open(state, size); int ambient = 0;
      for (Setting setting : options.session().settings()) if (setting.id.startsWith("power_environment:config.MUSIC_CONFIG.volume") && VolumeControls.accepts(setting)) {
        showOnly(options, setting.id); Object row = row(options, setting.id); ambient++;
        check(end - controlLeft(options, row) == 150, "legacy ambience preview shortened " + setting.id);
        auxiliary(options, row, false);
      }
      check(ambient == 4, "fixture did not check every legacy ambient volume");
      observations.add(Map.of("case", "volume-widths", "width", size[0], "individualSounds", sounds,
          "categories", categories, "legacyAmbience", ambient));
    }
  }

  private static void auxiliary(PowerOptionsScreen options, Object row, boolean action) throws Exception {
    int rowY = number(options, "top") + number(row, "y") - ((Number)get(options, "scroll")).intValue();
    String prefix = action ? "audioAction" : "volumeAuxiliary";
    int x = (int)call(options, prefix + "X", new Class<?>[] {row.getClass()}, row);
    int y = (int)call(options, prefix + "Y", new Class<?>[] {row.getClass(), int.class}, row, rowY);
    int h = (int)call(options, prefix + "Height", new Class<?>[] {row.getClass()}, row);
    if (number(row, "height") == 40) check(y + h <= controlY(options, row), "compact auxiliary overlaps volume track");
    else check(x + (action ? 44 : 20) <= controlLeft(options, row), "auxiliary consumes volume width");
  }

  private static void inputs(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) for (String id : List.of("audio.master", "audio.category.blocks", "audio.sound.random.click",
        "power_environment:config.MUSIC_CONFIG.volumeRainAmbient")) {
      var options = open(state, size);
      if (id.startsWith("audio.sound.")) call(options, "openAllSounds");
      else if (id.startsWith("power_environment:")) showOnly(options, id);
      Object row = row(options, id); reveal(options, row);
      int x = controlLeft(options, row), y = controlY(options, row), end = (int)call(options, "controlEnd", new Class<?>[] {Setting.class}, setting(options, id));
      click(options, end - 1, y + 8, 1); Object editor = get(options, "valueEditor");
      check((boolean)call(editor, "active"), "rightmost slider pixel missed exact entry: " + id);
      shot(mc, options, "probe-exact-" + id.replaceAll("[^a-zA-Z0-9]", "-"), size, -1, -1, false);
      check(number(editor, "x") == x && number(editor, "width") == end - x && number(editor, "y") == y,
          "exact entry changed outer slider bounds: " + id);
      Object geometry = call(editor, "geometry");
      check((int)call(geometry, "checkX") + (int)call(geometry, "checkWidth") == end, "check button leaves slider right edge");
      key(options, Keyboard.KEY_ESCAPE);
      Setting value = setting(options, id);
      for (boolean maximum : new boolean[] {false, true}) {
        int edge = maximum ? end - 1 : x;
        click(options, edge, y + 8, 0);
        check(get(options, "dragging") == value, "drawn endpoint did not acquire slider ownership: " + id);
        ((ScreenInput)(Object)options).power$release(edge, y + 8, 0);
        check(Math.abs(value.value.getAsDouble() - (maximum ? value.max : value.min)) < .00001, "drawn endpoint did not reach its bound: " + id);
      }
    }
  }

  private static void titles(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) {
      var options = open(state, size); call(options, "openAllSounds");
      shot(mc, options, "title-game-sounds", size, -1, -1, true);
      titlePixels(mc, options, "title-game-sounds", "All game sounds", size);
      Object heading = rows(options).stream().filter(row -> { try { return (boolean)call(row, "heading"); } catch (Exception e) { throw new RuntimeException(e); } }).findFirst().orElseThrow();
      check(call(heading, "group").equals("All"), "public group title replaced the All breadcrumb identifier");
      int headingX = number(options, "left"), headingY = number(options, "top") + number(heading, "y") + 8;
      var reference = new TextProbe("v All game sounds", headingX, headingY); reference.init(mc, size[0], size[1]);
      shot(mc, reference, "reference-game-sounds-heading", size, -1, -1, false);
      BufferedImage actualHeading = image("title-game-sounds", size), expectedHeading = image("reference-game-sounds-heading", size);
      int headingWidth = (int)call(options, "textWidth", new Class<?>[] {String.class}, "v All game sounds");
      for (int py = headingY; py < headingY + 8; py++) for (int px = headingX; px < headingX + headingWidth; px++)
        check((color(actualHeading, px, py) == 0xffffff) == (color(expectedHeading, px, py) == 0xffffff), "All game sounds group title is incomplete");
      check(setting(options, "audio.sound.random.click").group.equals("Game sounds / All"), "sound breadcrumb metadata changed");
      options = open(state, size); call(options, "editMusic"); shot(mc, options, "title-music", size, -1, -1, true);
      titlePixels(mc, options, "title-music", "Music", size);
    }
  }

  private static void titlePixels(Minecraft mc, PowerOptionsScreen options, String actual, String title, int[] size) throws Exception {
    int center = number(options, "origin") + number(options, "uiWidth") / 2;
    var probe = new TitleProbe(title, center); probe.init(mc, size[0], size[1]);
    shot(mc, probe, "reference-title-" + actual, size, -1, -1, false);
    BufferedImage image = image(actual, size), reference = image("reference-title-" + actual, size); int ink = 0;
    int width = (int)call(probe, "textWidth", new Class<?>[] {String.class}, title), left = center - width / 2;
    for (int py = 7; py < 15; py++) for (int px = left; px < left + width; px++) {
      boolean expected = color(reference, px, py) == 0xffffff;
      if (expected) ink++;
      check((color(image, px, py) == 0xffffff) == expected, "native full title differs from " + title);
    }
    check(ink > 0, "title reference contains no native font pixels");
  }

  private static void tooltips(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) {
      var options = open(state, size); call(options, "editMusic"); openPicker(options);
      Object popup = get(options, "choices"); List<String> names = labels(popup); int longIndex = names.indexOf(LONG);
      check(longIndex >= 0, "playlist picker lost the long saved title");
      for (int index = 0; index < names.size(); index++) {
        String help = (String)call(popup, "helpAt", new Class<?>[] {UiScreen.class, int.class}, options, index);
        int reserve = index == number(popup, "defaultIndex") ? (int)call(options, "textWidth", new Class<?>[] {String.class}, "Default") + 3 : 0;
        boolean truncated = (int)call(options, "textWidth", new Class<?>[] {String.class}, names.get(index)) > number(popup, "rowWidth") - 18 - reserve;
        check(help.equals(truncated ? names.get(index) : ""), "full-name tooltip does not match native truncation");
      }
      check(get(popup, "secondary") != null, "playlist picker did not opt into secondary actions");
      int x = number(popup, "x") + 12, y = popupY(popup, longIndex) + 8;
      call(get(popup, "hover"), "clear");
      shot(mc, new RenderProxy(options), "probe-tooltip-before", size, x, y, false);
      for (int tick = 0; tick < 13; tick++) call(popup, "tick");
      shot(mc, new RenderProxy(options), "playlist-full-name-tooltip", size, x, y, true);
      check(number(get(popup, "hover"), "ticks") == 13, "popup tooltip did not retain its hover delay");
      check(differences(image("probe-tooltip-before", size), image("playlist-full-name-tooltip", size)) > 40, "full playlist tooltip drew no native pixels after the delay");
    }
  }

  private static void modal(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) {
      var options = open(state, size); Object owner = row(options, "audio.playlist"); reveal(options, owner);
      click(options, controlLeft(options, owner) + 5, controlY(options, owner) + 8, 0);
      Object popup = get(options, "choices"); check((boolean)call(popup, "active"), "world playlist popup did not open");
      Object underlay = row(options, "audio.menuPlaylist");
      int x = controlLeft(options, underlay) - number(options, "left") > 40
          ? number(options, "left") + 4 : number(options, "right") - 32;
      int y = number(options, "top") + number(underlay, "y") - ((Number)get(options, "scroll")).intValue() + 5;
      check(y >= number(options, "top") && y < number(options, "bottom"), "modal underlay probe is outside the viewport");
      shot(mc, new RenderProxy(options), "probe-modal-idle", size, -1, -1, false);
      shot(mc, new RenderProxy(options), "playlist-popup-owner", size, x, y, true);
      BufferedImage idle = image("probe-modal-idle", size), over = image("playlist-popup-owner", size);
      int compared = 0;
      for (int py = y - 4; py < Math.min(y + 12, number(options, "bottom")); py++) for (int px = x; px < Math.min(x + 16, number(options, "right") - 14); px++) {
        if (px >= number(popup, "x") && px < number(popup, "x") + number(popup, "width")
            && py >= number(popup, "y") && py < number(popup, "y") + number(popup, "viewportHeight")) continue;
        compared++;
        check(color(idle, px, py) == color(over, px, py), "unrelated setting row retained hover beneath a modal popup");
      }
      check(compared > 0, "modal probe did not observe any exposed underlay pixels");
      check(call(popup, "editing", new Class<?>[] {Setting.class}, setting(options, "audio.playlist")).equals(true), "popup owner changed on underlay hover");
    }
  }

  private static void glyphs(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) {
      var probe = new GlyphProbe(); probe.init(mc, size[0], size[1]);
      shot(mc, probe, "probe-pause-canvas", size, -1, -1, false);
      for (int width : new int[] {18, 20}) for (int height : new int[] {18, 20})
        pausePixels(image("probe-pause-canvas", size), 20 + (width - 18) * 20, 24 + (height - 18) * 20, width, height);
    }
    transport(mc, state, true);
  }

  private static void transport(Minecraft mc, Release462Checks.State state, boolean verify) throws Exception {
    for (int[] size : SIZES) {
      var options = open(state, size); state.playback = true;
      AudioController.tick(mc); AudioController.playNow("music:calm1.ogg");
      check(AudioController.musicPlaying(), "silent transport fixture did not acquire playback identity");
      shot(mc, options, "audio-transport-playing", size, -1, -1, true);
      Object layout = call(options, "musicLayout");
      AudioToolbar.Button play = ((AudioToolbar.Layout)layout).buttons().get(0);
      if (verify) pausePixels(image("audio-transport-playing", size), play.x(), play.y(), play.width(), 18);
      for (boolean title : new boolean[] {false, true}) {
        Screen menu = title ? new TitleScreen() : new class_525(); mc.setScreen(menu); menu.init(mc, size[0], size[1]);
        String label = title ? "title-menu-transport-playing" : "pause-menu-transport-playing";
        shot(mc, menu, label, size, -1, -1, true);
        PauseMenuMusic controls = (PauseMenuMusic)get(menu, "power$music"); MenuMusicLayout bounds = controls.bounds();
        check(bounds != null, "native menu did not draw music controls");
        int x = number(controls, "controlsX");
        if (verify) pausePixels(image(label, size), x, bounds.y(), 20, 18);
      }
      AudioController.quiet();
    }
  }

  private static void pausePixels(BufferedImage image, int x, int y, int width, int height) {
    int left = x + (width - 8) / 2, top = y + (height - 7) / 2;
    for (int py = y + 2; py < y + height - 2; py++) for (int px = x + 2; px < x + width - 2; px++) {
      boolean expected = py >= top && py < top + 7 && (px == left + 1 || px == left + 2 || px == left + 5 || px == left + 6);
      check((color(image, px, py) == 0xdddddd) == expected, "pause glyph moved off its centered eight-pixel canvas");
    }
  }

  private static void openPicker(PowerOptionsScreen options) throws Exception { Object button = call(options, "playlistButton"); click(options, number(button, "x") + 3, number(button, "y") + 7, 0); }
  private static void query(PowerOptionsScreen options, String text) throws Exception { ((TextInput)get(options, "search")).setText(text); call(options, "layout"); }
  private static void showOnly(PowerOptionsScreen options, String id) throws Exception { set(options, "conflictIds", List.of(id)); set(options, "scroll", 0d); query(options, ""); }
  private static void show(PowerOptionsScreen options, String id) throws Exception { reveal(options, row(options, id)); }
  private static void reveal(PowerOptionsScreen options, Object row) throws Exception { set(options, "scroll", Math.max(0d, number(row, "y") - 32d)); call(options, "layout"); }
  private static int controlLeft(PowerOptionsScreen options, Object row) throws Exception { return (int)call(options, "mainControlLeft", new Class<?>[] {row.getClass()}, row); }
  private static int controlY(PowerOptionsScreen options, Object row) throws Exception { return number(options, "top") + number(row, "y") - ((Number)get(options, "scroll")).intValue() + (number(row, "height") == 40 ? 16 : 3); }
  private static List<?> rows(PowerOptionsScreen options) throws Exception { return (List<?>)get(options, "rows"); }
  private static Object row(PowerOptionsScreen options, String id) throws Exception { for (Object row : rows(options)) { Setting setting = (Setting)call(row, "setting"); if (setting != null && setting.id.equals(id)) return row; } throw new AssertionError("missing row " + id); }
  private static Object firstVolumeRow(PowerOptionsScreen options, String prefix) throws Exception { for (Object row : rows(options)) { Setting setting = (Setting)call(row, "setting"); if (setting != null && setting.id.startsWith(prefix) && VolumeControls.accepts(setting)) return row; } throw new AssertionError("missing volume row " + prefix); }
  private static Setting setting(PowerOptionsScreen options, String id) { return options.session().settings().stream().filter(s -> s.id.equals(id)).findFirst().orElseThrow(); }
  @SuppressWarnings("unchecked") private static List<String> labels(Object popup) throws Exception { return (List<String>)get(popup, "labels"); }
  private static int popupY(Object popup, int index) throws Exception { return number(popup, "y") + (int)call(get(popup, "layout"), "top", new Class<?>[] {int.class}, index) - ((Number)get(popup, "scroll")).intValue(); }
  private static void click(Screen screen, int x, int y, int button) { ((ScreenInput)(Object)screen).power$click(x, y, button); }
  private static void key(Screen screen, int key) { ((ScreenInput)(Object)screen).power$key('\0', key); }
  private static void shot(Minecraft mc, Screen screen, String label, int[] size, int mx, int my, boolean actual) throws Exception { String name = label + "-" + size[0] + ".png"; SettingsSnapshot.render(mc, screen, size[0], size[1], REPORT.resolve(name), mx, my); if (actual && !captures.contains(name)) captures.add(name); }
  private static BufferedImage image(String label, int[] size) throws Exception { return ImageIO.read(REPORT.resolve(label + "-" + size[0] + ".png").toFile()); }
  private static int color(BufferedImage image, int x, int y) { return image.getRGB(x * 2, y * 2) & 0xffffff; }
  private static int differences(BufferedImage first, BufferedImage second) { int count = 0; for (int y = 0; y < first.getHeight(); y++) for (int x = 0; x < first.getWidth(); x++) if (first.getRGB(x, y) != second.getRGB(x, y)) count++; return count; }
  private static int number(Object owner, String name) throws Exception { try { return ((Number)call(owner, name)).intValue(); } catch (NoSuchMethodException missing) { return ((Number)get(owner, name)).intValue(); } }
  private static Field field(Class<?> type, String name) throws Exception { for (; type != null; type = type.getSuperclass()) try { var field = type.getDeclaredField(name); field.setAccessible(true); return field; } catch (NoSuchFieldException ignored) {} throw new NoSuchFieldException(name); }
  private static Object get(Object owner, String name) throws Exception { return field(owner.getClass(), name).get(owner); }
  private static void set(Object owner, String name, Object value) throws Exception { field(owner.getClass(), name).set(owner, value); }
  private static Object call(Object owner, String name) throws Exception { return call(owner, name, new Class<?>[0]); }
  private static Object call(Object owner, String name, Class<?>[] types, Object... args) throws Exception { for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) try { var method = type.getDeclaredMethod(name, types); method.setAccessible(true); return method.invoke(owner, args); } catch (NoSuchMethodException ignored) {} throw new NoSuchMethodException(name); }
  private static final class RenderProxy extends Screen { private final Screen delegate; RenderProxy(Screen delegate) { this.delegate = delegate; } @Override public void render(int x, int y, float delta) { delegate.render(x, y, delta); } }
  private static final class TitleProbe extends UiScreen { private final String title; private final int center; TitleProbe(String title, int center) { this.title = title; this.center = center; } @Override public void render(int x, int y, float delta) { fill(0, 0, width, height, 0xff000000); drawCenteredTextWithShadow(textRenderer, title, center, 7, 0xffffff); } }
  private static final class TextProbe extends UiScreen { private final String label; private final int x, y; TextProbe(String label, int x, int y) { this.label = label; this.x = x; this.y = y; } @Override public void render(int mx, int my, float delta) { fill(0, 0, width, height, 0xff000000); text(label, x, y, 0xffffff); } }
  private static final class GlyphProbe extends UiScreen { @Override public void render(int x, int y, float delta) { fill(0, 0, width, height, 0xff000000); for (int w : new int[] {18, 20}) for (int h : new int[] {18, 20}) iconButton("pause", 20 + (w - 18) * 20, 24 + (h - 18) * 20, w, h, -1, -1, true, false); } }
}
