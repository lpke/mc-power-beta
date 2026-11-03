package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.Validation.*;

import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/** Native picker paint, hit bounds and isolated nested drafts at compact/wide sizes. */
final class Picker474Checks {
  private static final Path REPORT = Path.of("power-beta-data/reports/picker-474");

  static void run(Minecraft mc, String action) throws Exception {
    check(action.equals("capture") || action.equals("check"), "use picker-474 capture|check");
    check(Display.isActive() && mc.options.soundVolume == 0 && mc.options.musicVolume == 0,
        "focus a muted disposable client");
    Files.createDirectories(REPORT);
    PowerConfig.flushPending(); byte[] saved = Files.readAllBytes(PowerConfig.path());
    Screen previous = mc.currentScreen;
    int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      for (int[] size : new int[][]{{320, 240}, {854, 480}}) {
        var options = new PowerOptionsScreen(previous); options.init(mc, size[0], size[1]);
        try {
          var setting = find(options.session(), "veinMining.tools");
          var original = setting.value.deepCopy();
          var editor = new BlockRulesScreen(options, setting, true); editor.init(mc, size[0], size[1]);
          int tools = (int)call(editor, "toolsX");
          ((ScreenInput)(Object)editor).power$click(tools + 5, 72, 0);
          check(mc.currentScreen instanceof ItemPickerScreen, "Required tools did not open the shared picker");
          var nested = (ItemPickerScreen)mc.currentScreen; nested.init(mc, size[0], size[1]);
          check(field(nested, "title").equals("Required tools"), "wrong nested picker");
          paint(mc, nested, size, "required-tools", action.equals("check"));
          nested.removed();
          if (action.equals("check")) {
            var before = ((com.google.gson.JsonObject)field(editor, "draft")).deepCopy();
            int left = (int)call(nested, "left");
            ((ScreenInput)(Object)nested).power$click(left + 32, 74, 0);
            check(before.equals(field(editor, "draft")) && original.equals(setting.value), "picker click escaped its isolated draft");
            ((ScreenInput)(Object)nested).power$key('\0', Keyboard.KEY_ESCAPE);
            check((boolean)call(field(nested, "exitPrompt"), "active"), "dirty picker did not confirm its exit");
            BufferedImage modal = shot(mc, nested, size, "required-tools-modal", left + 2, 67);
            BufferedImage quiet = shot(mc, nested, size, "required-tools-modal-quiet", -1, -1);
            check(modal.getRGB((left + 1) * 2, 67 * 2) == quiet.getRGB((left + 1) * 2, 67 * 2),
                "modal warning retained underlay row hover");
            ((ScreenInput)(Object)nested).power$key('\0', Keyboard.KEY_ESCAPE);
            check(before.equals(field(editor, "draft")), "cancelled warning changed the outer draft");
          }
          editor.removed();
          for (var domain : ItemCatalogue.Domain.values()) for (boolean multiple : new boolean[]{false, true}) {
            var picker = new ItemPickerScreen(options, "Choose item", domain, List.of(), multiple, values -> {
              throw new AssertionError("render or draft selection accepted gameplay data");
            });
            picker.init(mc, size[0], size[1]);
            paint(mc, picker, size, domain.name().toLowerCase(Locale.ROOT) + "-" + multiple, action.equals("check"));
            picker.removed();
          }
          check(original.equals(setting.value), "picker checks changed the setting");
        } finally { options.session().discard(); options.removed(); }
      }
      check(Arrays.equals(saved, Files.readAllBytes(PowerConfig.path())), "picker checks changed saved preferences");
      log("PICKER474 " + action + " COMPLETE: Required tools, all domains, single/multiple, selected hover, full bounds and isolated drafts");
    } finally {
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glPopAttrib(); GL11.glMatrixMode(mode); mc.setScreen(previous);
    }
  }

  @SuppressWarnings("unchecked")
  private static void paint(Minecraft mc, ItemPickerScreen picker, int[] size, String name, boolean verify) throws Exception {
    int left = (int)call(picker, "left"), right = (int)call(picker, "right");
    var selected = (Set<String>)field(picker, "selected"); var original = new LinkedHashSet<>(selected);
    var shown = (List<ItemCatalogue.Entry>)field(picker, "shown");
    check(!shown.isEmpty() && ((TextInput)field(picker, "search")).focused, "picker is empty or search did not focus");
    selected.clear();
    BufferedImage plain = shot(mc, picker, size, name + "-plain", -1, -1);
    BufferedImage hover = shot(mc, picker, size, name + "-hover", left + 35, 74);
    selected.add(shown.get(0).value());
    BufferedImage chosen = shot(mc, picker, size, name + "-selected-hover", left + 35, 74);
    if (verify) {
      for (int x : new int[]{left + 1, right - 11}) for (int y : new int[]{67, 89}) {
        int base = plain.getRGB(x * 2, y * 2), actual = hover.getRGB(x * 2, y * 2);
        for (int shift : new int[]{0, 8, 16}) {
          int expected = Math.round(((base >> shift) & 255) * 159f / 255);
          check(Math.abs(((actual >> shift) & 255) - expected) <= 1, "picker does not use Options' full-row 0x60000000 hover at " + name);
        }
        check(actual == chosen.getRGB(x * 2, y * 2), "selection overrode the hover paint at " + name);
      }
      BufferedImage next = shot(mc, picker, size, name + "-next-row", left + 35, 90);
      check(plain.getRGB((left + 1) * 2, 89 * 2) != next.getRGB((left + 1) * 2, 89 * 2), "selected row lost its unhovered selection fill");
    }
    selected.clear(); selected.addAll(original);
  }

  private static BufferedImage shot(Minecraft mc, Screen screen, int[] size, String name, int x, int y) throws Exception {
    Path path = REPORT.resolve(name + "-" + size[0] + ".png");
    SettingsSnapshot.render(mc, screen, size[0], size[1], path, x, y);
    return ImageIO.read(path.toFile());
  }
}
