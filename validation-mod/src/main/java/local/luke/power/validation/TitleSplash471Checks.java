package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import local.luke.power.config.*;
import local.luke.power.config.backend.LogoBackend;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.TitleSplash471Access;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.TitleScreen;
import org.lwjgl.opengl.Display;

/** Native title rendering on a neutral background isolates splash pixels from panorama animation. */
final class TitleSplash471Checks {
  private static final String ID = "logo.showSplashText";
  private static final Path REPORT = Path.of("power-beta-data/reports/release-471/title-splash");
  private static final int[][] SIZES = {{854, 480}, {320, 240}};
  private static final String[] SPLASHES = {"Power Beta 4.7.1!", "Happy birthday, ez!", "Happy birthday, Notch!", "Merry X-mas!", "Happy new year!"};
  private TitleSplash471Checks() {}

  static void run(Minecraft mc, String action) throws Exception {
    check(Set.of("capture", "run").contains(action), "use title-splash-471 capture|run");
    check(mc.world == null && mc.currentScreen instanceof TitleScreen, "run from the disposable client's title screen");
    check(Display.isActive() && mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "focus and mute the disposable client");
    check(Display.getWidth() >= 1708 && Display.getHeight() >= 960, "resize the disposable display to at least 1708x960");
    Files.createDirectories(REPORT); failures = 0;
    Object config = Class.forName("local.luke.power.title.TitleFeatures").getField("CONFIG").get(null);
    JsonObject original = (JsonObject)config.getClass().getMethod("snapshot").invoke(config);
    Object ui = Class.forName("local.luke.power.controls.ControlFeatures").getField("USER_INTERFACE_CONFIG").get(null);
    Object panorama = ui.getClass().getField("panoramaConfig").get(ui);
    Field panoramaEnabled = panorama.getClass().getField("enablePanorma");
    Object previousPanorama = panoramaEnabled.get(panorama);
    // The Controls panorama wrapper skips the subclass's neutral background.
    // Disable that wrapper only in memory while comparing native title pixels.
    panoramaEnabled.set(panorama, false);
    try (var state = new Release462Checks.State(mc)) {
      try {
        // Freeze only the fixture's retro movement, retaining its complete custom design.
        new LogoBackend().apply(Map.of("logo.logo.animation.enabled", new JsonPrimitive(false)));
        capture(mc, state);
        if (action.equals("run")) {
          test("TITLE SPLASH471 native pixels in every title style and compact layout", () -> pixels(mc));
          test("TITLE SPLASH471 custom and native special-date text survives disable/re-enable", () -> special(mc));
          test("TITLE SPLASH471 defaults, draft Cancel and Apply use shared settings", () -> settings(mc));
          JsonObject after = (JsonObject)config.getClass().getMethod("snapshot").invoke(config);
          for (String key : original.keySet()) {
            if (Set.of("style", "showSplashText", "logo").contains(key)) continue;
            check(original.get(key).equals(after.get(key)), "splash tests changed unrelated title data " + key);
          }
          JsonObject beforeLogo = original.getAsJsonObject("logo").deepCopy(), afterLogo = after.getAsJsonObject("logo").deepCopy();
          beforeLogo.getAsJsonObject("animation").remove("enabled"); afterLogo.getAsJsonObject("animation").remove("enabled");
          check(beforeLogo.equals(afterLogo), "splash tests changed custom lettering, rotation or shadow");
        }
      } finally { reload(config, original); }
    } finally { panoramaEnabled.set(panorama, previousPanorama); }
    Files.writeString(REPORT.resolve(action + ".json"), "{\"failures\":" + failures + "}");
    log("TITLE SPLASH471 " + action + " failures=" + failures + " reports=" + REPORT.toAbsolutePath());
    check(failures == 0, "title-splash-471 failures=" + failures);
  }

  private static void capture(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int style = 0; style < 5; style++) for (int[] size : SIZES) {
      TitleScreen title = title(mc, size, SPLASHES[0]);
      for (boolean show : new boolean[]{true, false}) {
        apply(style, show);
        SettingsSnapshot.render(mc, title, size[0], size[1], file(style, size, show));
      }
    }
    for (int[] size : SIZES) {
      var options = state.open(size, local.luke.power.audio.MusicPlaylist.BETA, false);
      set(options, "page", "Interface"); ((TextInput)get(options, "search")).setText("Show splash text");
      ((Set<?>)get(options, "collapsed")).clear(); call(options, "layout");
      SettingsSnapshot.render(mc, options, size[0], size[1], REPORT.resolve("setting-" + size[0] + ".png"));
    }
  }
  private static void pixels(Minecraft mc) throws Exception {
    for (int style = 0; style < 5; style++) for (int[] size : SIZES) {
      BufferedImage on = ImageIO.read(file(style, size, true).toFile()), off = ImageIO.read(file(style, size, false).toFile());
      check(differences(on, off) > 20, "splash did not appear for style=" + style + " width=" + size[0]);
      apply(style, false); TitleScreen title = title(mc, size, "");
      Path blank = REPORT.resolve("blank-" + style + "-" + size[0] + ".png");
      SettingsSnapshot.render(mc, title, size[0], size[1], blank);
      check(differences(off, ImageIO.read(blank.toFile())) == 0,
          "hidden splash changed other native title pixels style=" + style + " width=" + size[0]);
    }
  }
  private static void special(Minecraft mc) throws Exception {
    int[] size = SIZES[1]; TitleScreen title = title(mc, size, "");
    TitleSplash471Access text = (TitleSplash471Access)(Object)title;
    for (int i = 0; i < SPLASHES.length; i++) {
      String splash = SPLASHES[i]; text.power$splashText(splash);
      apply(1, false); Path off = REPORT.resolve("special-" + i + "-off.png"); SettingsSnapshot.render(mc, title, size[0], size[1], off);
      check(splash.equals(text.power$splashText()), "disabling replaced the native custom/special-date text");
      apply(1, true); Path on = REPORT.resolve("special-" + i + "-on.png"); SettingsSnapshot.render(mc, title, size[0], size[1], on);
      check(splash.equals(text.power$splashText()) && differences(ImageIO.read(on.toFile()), ImageIO.read(off.toFile())) > 20,
          "re-enabling lost native custom/special-date text " + splash);
    }
  }
  private static void settings(Minecraft mc) throws Exception {
    for (var base : DefaultBases.Base.values()) check(DefaultBases.value(base, ID, new JsonPrimitive(false)).getAsBoolean(), "splash default off in " + base);
    apply(1, true); ConfigSession session = SettingsRegistry.open(mc); Setting splash = find(session);
    check(splash.label.equals("Show splash text") && splash.page.equals("Interface") && splash.group.equals("Title screen")
        && splash.kind == Setting.Kind.BOOLEAN && splash.powerBetaDefault.getAsBoolean() && !splash.restart, "splash setting metadata/default mismatch");
    byte[] before = Files.readAllBytes(local.luke.power.storage.PowerConfig.path());
    splash.value = new JsonPrimitive(false); session.preview();
    check(shown() && Arrays.equals(before, Files.readAllBytes(local.luke.power.storage.PowerConfig.path())), "manual splash draft applied before Apply");
    session.discard(); check(shown() && splash.value.getAsBoolean(), "Cancel retained the splash draft");
    splash.value = new JsonPrimitive(false); session.save(net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir());
    check(!shown() && !local.luke.power.storage.PowerConfig.section("title").get("showSplashText").getAsBoolean(), "Apply did not persist/reload splash visibility");
    ConfigSession reopened = SettingsRegistry.open(mc); check(!find(reopened).value.getAsBoolean(), "reopened settings lost applied splash visibility");
    find(reopened).value = new JsonPrimitive(true); reopened.save(net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir());
    check(shown(), "splash could not be re-enabled through Apply");
  }
  private static TitleScreen title(Minecraft mc, int[] size, String splash) throws Exception {
    TitleScreen title = new TitleScreen() { @Override public void renderBackground() { fill(0, 0, width, height, 0xff202020); } };
    title.init(mc, size[0], size[1]); ((TitleSplash471Access)(Object)title).power$splashText(splash); return title;
  }
  private static void apply(int style, boolean show) throws Exception { new LogoBackend().apply(Map.of("logo.style", new JsonPrimitive(style), ID, new JsonPrimitive(show))); }
  private static boolean shown() throws Exception { Object key = Class.forName("local.luke.power.title.TitleFeatures").getField("showSplashText").get(null); return (boolean)key.getClass().getMethod("get").invoke(key); }
  private static Path file(int style, int[] size, boolean show) { return REPORT.resolve("title-" + style + "-" + size[0] + "-" + (show ? "on" : "off") + ".png"); }
  private static Setting find(ConfigSession session) { return session.settings().stream().filter(s -> s.id.equals(ID)).findFirst().orElseThrow(); }
  private static int differences(BufferedImage a, BufferedImage b) { check(a.getWidth() == b.getWidth() && a.getHeight() == b.getHeight(), "title capture sizes differ"); int count = 0; for (int y = 0; y < a.getHeight(); y++) for (int x = 0; x < a.getWidth(); x++) if (a.getRGB(x, y) != b.getRGB(x, y)) count++; return count; }
  private static void reload(Object config, JsonObject data) throws Exception {
    config.getClass().getMethod("replace", JsonObject.class).invoke(config, data);
    for (Field field : Class.forName("local.luke.power.title.TitleFeatures").getFields()) if (field.getType().getSimpleName().equals("ConfigKey")) {
      Object key = field.get(null); key.getClass().getMethod("load").invoke(key);
    }
  }
  private static Field field(Class<?> type, String name) throws Exception { Field f = type.getDeclaredField(name); f.setAccessible(true); return f; }
  private static Object get(Object value, String name) throws Exception { return field(value.getClass(), name).get(value); }
  private static void set(Object value, String name, Object data) throws Exception { field(value.getClass(), name).set(value, data); }
  private static void call(Object value, String name) throws Exception { Method m = value.getClass().getDeclaredMethod(name); m.setAccessible(true); m.invoke(value); }
}
