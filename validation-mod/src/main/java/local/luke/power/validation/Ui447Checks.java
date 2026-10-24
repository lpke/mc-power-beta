package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import local.luke.power.config.*;
import local.luke.power.creative.CreativeTabsData;
import local.luke.power.creativeui.CreativeTabsBackend;
import local.luke.power.map.*;
import local.luke.power.recipes.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/** Native visual checks only; install in a muted disposable client. */
final class Ui447Checks {
  static void run(Minecraft mc, String action) throws Exception {
    check(action.equals("capture") || action.equals("run"), "use ui-447 capture|run");
    check(mc.world != null && mc.player != null && !mc.world.isRemote && Display.isActive(), "focus a disposable world");
    check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "mute the disposable client");
    boolean verify = action.equals("run");
    Path directory = Path.of("power-beta-data/reports/ui-447").resolve(action); Files.createDirectories(directory);
    MapSettings before = MapConfig.copy();
    PowerConfig.flushPending(); byte[] bytes = Files.readAllBytes(PowerConfig.path());
    try (var state = new Recipe402Checks.State(mc)) {
      for (int width : List.of(320, 640)) {
        state.reset(width);
        var recipe = RecipeBookConfig.copy(); recipe.enabled = false; RecipeBookConfig.preview(recipe);
        RecipeBooks.book(mc.currentScreen).tick();
        var style = MapConfig.copy(); style.minimap = style.showInInventory = true;
        style.items = style.hostile = style.animals = true; style.waypoints = style.worldWaypoints = false;
        style.caveMode = 0; style.corner = 1; style.size = 108; style.offsetX = style.offsetY = 6;
        MapConfig.preview(style); Maps.tick(mc); Mouse.setGrabbed(false);
        var layoutMethod = MinimapQuickMenu.class.getDeclaredMethod("layout", Minecraft.class); layoutMethod.setAccessible(true);
        var layout = (MinimapQuickMenuLayout.Layout)layoutMethod.invoke(null, mc);
        var gear = layout.gear();
        MinimapQuickMenu.beginFrame();
        check(MinimapQuickMenu.mouse(mc, gear.x() + 8, gear.y() + 8, 0, true), "could not open native minimap menu");
        MinimapQuickMenu.mouse(mc, gear.x() + 8, gear.y() + 8, 0, false);
        Screen host = mc.currentScreen;
        Screen plate = new Screen() { @Override public void render(int x, int y, float tick) {
          host.render(x, y, tick); MinimapQuickMenu.renderMenu(mc, x, y);
        }};
        plate.init(mc, width, 360);
        BufferedImage menu;
        try { menu = render(mc, plate, width, directory.resolve("minimap-" + width + ".png")); }
        finally { var close = MinimapQuickMenu.class.getDeclaredMethod("close"); close.setAccessible(true); close.invoke(null); }
        if (verify) {
          for (int i = 0; i < MinimapQuickMenuLayout.ROWS; i++) {
            var row = layout.row(i); int count = 0, minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = -1, maxY = -1;
            double sumX = 0, sumY = 0;
            for (int y = (row.y() + 5) * 2; y < (row.y() + 15) * 2; y++)
              for (int x = (row.x() + 3) * 2; x < (row.x() + 13) * 2; x++)
                if ((menu.getRGB(x, y) & 0xffffff) == 0xffffff) {
                  count++; sumX += x + .5; sumY += y + .5;
                  minX = Math.min(minX, x); maxX = Math.max(maxX, x); minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                }
            if (i >= 2) check(count == 0, "unchecked minimap box contains a mark");
            else {
              double cx = (row.x() + 8) * 2d, cy = (row.y() + 10) * 2d;
              check(count > 0 && Math.abs(sumX / count - cx) < .001 && Math.abs(sumY / count - cy) < .001,
                  "minimap tick ink is not centered");
              check((minX + maxX + 1) / 2d == cx && (minY + maxY + 1) / 2d == cy,
                  "minimap tick bounds are not centered");
            }
          }
          log("PASS UI447 checked/unchecked tick centering at " + width);
        }
        var session = new ConfigSession(); CreativeTabsBackend.register(session);
        var setting = session.settings().get(0); setting.value = CreativeTabsData.defaults();
        var original = setting.value.deepCopy();
        var editor = new CreativeTabsScreen(host, setting, () -> { throw new AssertionError("Visual fixture published a draft"); });
        mc.setScreen(editor); editor.init(mc, width, 360);
        BufferedImage tabs = render(mc, editor, width, directory.resolve("creative-" + width + ".png"));
        if (verify) {
          int fill = (int)call(editor, "fillGroupX");
          var buttonX = CreativeTabsScreen.class.getDeclaredMethod("tabButtonX", int.class); buttonX.setAccessible(true);
          for (int button = 0; button < 2; button++) {
            int other = (int)buttonX.invoke(editor, button);
            for (int y = 60; y < 100; y++) for (int dx = 0; dx < 2; dx++)
              check(tabs.getRGB((fill + 3) * 2 + dx, y) == tabs.getRGB((other + 3) * 2 + dx, y),
                  "Fill group button does not match tab-button top/bottom borders");
          }
          editor.layerPress(fill + 3, 49, 0);
          check((boolean)field(editor, "nativeGroups"), "Fill group bottom edge did not open dropdown");
          editor.layerKey('\0', org.lwjgl.input.Keyboard.KEY_ESCAPE);
          check(!(boolean)field(editor, "nativeGroups"), "Escape did not close dropdown");
          check(setting.value.equals(original), "Dropdown changed the isolated draft");
          log("PASS UI447 button geometry and bottom-edge input at " + width);
        }
        editor.removed();
      }
    } finally { MapConfig.preview(before); }
    check(Arrays.equals(bytes, Files.readAllBytes(PowerConfig.path())), "Visual checks changed saved settings");
    log("UI447 " + action + " completed");
  }
  private static BufferedImage render(Minecraft mc, Screen screen, int width, Path path) throws Exception {
    int nativeScale = mc.options.guiScale;
    Class<?> options = Class.forName("local.luke.power.controls.util.ModOptions");
    var scaleField = options.getField("guiScale"); var realScaleField = options.getField("realGuiScale");
    float scale = scaleField.getFloat(null), realScale = realScaleField.getFloat(null);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      mc.options.guiScale = 2; scaleField.setFloat(null, .25f); realScaleField.setFloat(null, .25f);
      SettingsSnapshot.render(mc, screen, width, 360, path); return ImageIO.read(path.toFile());
    } finally {
      mc.options.guiScale = nativeScale; scaleField.setFloat(null, scale); realScaleField.setFloat(null, realScale);
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix(); GL11.glPopAttrib();
    }
  }
}
