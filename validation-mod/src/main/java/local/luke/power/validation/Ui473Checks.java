package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.Validation.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import local.luke.power.config.Setting;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/** Row paint and native multiplayer navigation in a muted disposable title screen. */
final class Ui473Checks {
  private static final Path REPORT = Path.of("power-beta-data/reports/ui-473");
  private Ui473Checks() {}

  static void run(Minecraft mc) throws Exception {
    check(mc.world == null && Display.isActive(), "focus the disposable client's title screen");
    check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "mute the disposable client");
    Files.createDirectories(REPORT);
    PowerConfig.flushPending(); byte[] config = Files.readAllBytes(PowerConfig.path());
    Screen previous = mc.currentScreen; String lastServer = mc.options.lastServer;
    int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      for (int[] size : new int[][]{{320, 240}, {854, 480}}) {
        var options = new PowerOptionsScreen(previous); options.init(mc, size[0], size[1]);
        Setting setting = find(options.session(), "veinMining.tools");
        var editor = new BlockRulesScreen(options, setting, true); editor.init(mc, size[0], size[1]);
        check(setting.value.getAsJsonObject().size() > 0, "use a disposable clone with vein block defaults");
        var original = setting.value.deepCopy();
        int left = (int)call(editor, "left"), right = (int)call(editor, "right"), tools = (int)call(editor, "toolsX");
        BufferedImage plain = shot(mc, editor, size, "blocks-plain", -1, -1);
        BufferedImage label = shot(mc, editor, size, "blocks-label", left + 30, 73);
        BufferedImage required = shot(mc, editor, size, "blocks-tools", tools + 5, 73);
        BufferedImage remove = shot(mc, editor, size, "blocks-remove", right - 25, 73);
        for (int x : new int[]{left + 1, right - 11}) {
          int base = plain.getRGB(x * 2, 63 * 2), hover = label.getRGB(x * 2, 63 * 2);
          for (int shift : new int[]{0, 8, 16}) {
            int expected = Math.round(((base >> shift) & 255) * 159f / 255);
            check(Math.abs(((hover >> shift) & 255) - expected) <= 1, "row does not use settings' 0x60000000 hover paint");
          }
          check(hover == required.getRGB(x * 2, 63 * 2) && hover == remove.getRGB(x * 2, 63 * 2),
              "row hover disappears over required-tools/remove controls");
        }
        check(setting.value.equals(original), "hover changed the isolated block draft");
        editor.removed();
        vehicleAudio(mc, size, previous);
        navigation(mc, size);
        log("PASS UI473 row hover and one-level multiplayer Escape at " + size[0]);
      }
      check(lastServer.equals(mc.options.lastServer), "Escape changed the last server");
      check(Arrays.equals(config, Files.readAllBytes(PowerConfig.path())), "UI checks changed saved settings");
      log("UI473 COMPLETE");
    } finally {
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glPopAttrib(); GL11.glMatrixMode(mode); mc.setScreen(previous);
    }
  }

  private static void vehicleAudio(Minecraft mc, int[] size, Screen previous) throws Exception {
    var options = new PowerOptionsScreen(previous); options.init(mc, size[0], size[1]);
    try {
      field(options, "page", "Audio"); ((Set<?>)field(options, "collapsed")).clear();
      field(options, "changedOnly", false); field(options, "libraryOpen", false); field(options, "allSounds", false);
      search(options, "Boat and minecart hit sounds");
      Setting setting = find(options.session(), "audio.vehicleAttackSounds"); Object target = null;
      for (Object row : (List<?>)field(options, "rows")) {
        Setting value = (Setting)call(row, "setting"); if (value != null && value.id.equals(setting.id)) { target = row; break; }
      }
      check(target != null && setting.kind == Setting.Kind.BOOLEAN && setting.group.equals("Extra sounds"), "vehicle audio setting is absent from Extra sounds");
      field(options, "scroll", Math.max(0d, (double)(int)call(target, "y") - 24)); call(options, "layout");
      int rowY = (int)call(options, "top") + (int)call(target, "y") - ((Number)field(options, "scroll")).intValue();
      int height = (int)call(target, "height"), cy = rowY + (height == 40 ? 16 : 3);
      var leftMethod = PowerOptionsScreen.class.getDeclaredMethod("controlLeft", target.getClass()); leftMethod.setAccessible(true);
      var toggleMethod = PowerOptionsScreen.class.getDeclaredMethod("mainControlLeft", target.getClass()); toggleMethod.setAccessible(true);
      var endMethod = PowerOptionsScreen.class.getDeclaredMethod("controlEnd", Setting.class); endMethod.setAccessible(true);
      var previewMethod = PowerOptionsScreen.class.getDeclaredMethod("previewId", Setting.class); previewMethod.setAccessible(true);
      int preview = (int)leftMethod.invoke(options, target), toggle = (int)toggleMethod.invoke(options, target), end = (int)endMethod.invoke(options, setting);
      check(preview >= 0 && preview + 20 <= toggle && toggle < end && end <= size[0], "vehicle preview/toggle bounds overlap or leave screen");
      check(rowY >= (int)call(options, "top") && cy + 18 <= (int)call(options, "bottom"), "vehicle controls leave visible row viewport");
      check("power_beta:entity.player.attack.strong".equals(previewMethod.invoke(options, setting)), "vehicle setting has no registered thunk preview");
      var before = setting.value.deepCopy();
      shot(mc, options, size, "vehicle-audio-setting", -1, -1);
      shot(mc, options, size, "vehicle-audio-setting-help", (int)call(options, "left") + 5, rowY + 5);
      check(setting.value.equals(before), "vehicle setting captures changed the saved preference");
      log("PASS UI473 vehicle audio setting at " + size[0] + " preview=" + preview + " toggle=" + toggle + " end=" + end + " row=" + rowY);
    } finally { options.session().discard(); options.removed(); }
  }

  private static void navigation(Minecraft mc, int[] size) throws Exception {
    Screen title = new TitleScreen(); mc.setScreen(title);
    Class<?> type = Class.forName("local.luke.power.client_fixes.client.gui.multiplayer.MultiplayerScreen");
    Screen menu = (Screen)type.getConstructor(Screen.class).newInstance(title);
    mc.setScreen(menu); menu.init(mc, size[0], size[1]);
    Path servers = Minecraft.getRunDirectory().toPath().resolve("servers.dat");
    byte[] saved = Files.readAllBytes(servers);
    for (int button : new int[]{1, 2}) for (boolean dirty : new boolean[]{false, true}) {
      var action = ((ScreenInput)menu).power$buttons().get(button);
      ((ScreenInput)menu).power$click(action.x + 5, action.y + 5, 0);
      Screen child = mc.currentScreen;
      check(child != menu && child.getClass().getSimpleName().equals(button == 1 ? "DirectConnectScreen" : "EditServerScreen"), "wrong child menu");
      child.init(mc, size[0], size[1]);
      if (button == 2) ((ScreenInput)child).power$click(size[0] / 2 - 90, dirty ? 67 : 113, 0);
      if (dirty) {
        if (button == 1) ((TextFieldWidget)field(child, "addressField")).setText("unsaved.invalid:25565");
        else {
          ((TextFieldWidget)field(child, "nameTextField")).setText("Unsaved server");
          ((TextFieldWidget)field(child, "ipTextField")).setText("unsaved.invalid");
        }
      }
      shot(mc, child, size, (button == 1 ? "direct" : "add") + (dirty ? "-draft" : "-clean"), -1, -1);
      ((ScreenInput)child).power$key('\0', Keyboard.KEY_ESCAPE);
      check(mc.currentScreen == menu, "Escape skipped the Multiplayer parent");
      check(!Keyboard.areRepeatEventsEnabled(), "child left keyboard repeats enabled");
      check(Arrays.equals(saved, Files.readAllBytes(servers)), "Escape saved a server draft");
      menu.init(mc, size[0], size[1]);
    }
    ((ScreenInput)menu).power$key('\0', Keyboard.KEY_ESCAPE);
    check(mc.currentScreen == title, "Multiplayer Escape did not return to its title parent");
  }

  private static BufferedImage shot(Minecraft mc, Screen screen, int[] size, String name, int mx, int my) throws Exception {
    Path file = REPORT.resolve(name + "-" + size[0] + ".png");
    SettingsSnapshot.render(mc, screen, size[0], size[1], file, mx, my);
    return ImageIO.read(file.toFile());
  }
}
