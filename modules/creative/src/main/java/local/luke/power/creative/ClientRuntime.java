package local.luke.power.creative;

import java.lang.reflect.*;
import local.luke.power.creative.api.ModePlayer;
import local.luke.power.creative.config.Config;
import local.luke.power.creative.ui.ModePickerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.level.Level;
import org.lwjgl.opengl.Display;

public final class ClientRuntime {
  private static Minecraft minecraft;
  private static Level world;
  private static boolean modifierDown, pickerDown, debugBefore;
  private static int speedTicks;
  private static Field freecam;
  private static Method freecamActive;

  static {
    try {
      freecam = Class.forName("local.luke.power.camera.Freecam").getField("freecamController");
      freecamActive = freecam.getType().getMethod("isActive");
    } catch (ReflectiveOperationException ignored) {
    }
  }

  private ClientRuntime() {}

  public static Minecraft minecraft() {
    return minecraft;
  }

  public static boolean local(Minecraft mc) {
    return mc != null
        && mc.level != null
        && !mc.level.isRemote
        && mc.player != null
        && mc.player.level == mc.level
        && !mc.player.removed
        && mc.player.health > 0;
  }

  public static boolean freecam() {
    if (freecam == null || freecamActive == null) return false;
    try {
      Object c = freecam.get(null);
      return c != null && Boolean.TRUE.equals(freecamActive.invoke(c));
    } catch (ReflectiveOperationException e) {
      return true;
    }
  }

  public static boolean active(Minecraft mc) {
    return local(mc)
        && mc.currentScreen == null
        && mc.hasFocus
        && Display.isActive()
        && mc.viewEntity == mc.player
        && !freecam();
  }

  public static void tick(Minecraft mc) {
    minecraft = mc;
    if (world != mc.level) {
      world = mc.level;
      Modes.reset();
      FlightController.reset();
    }
    boolean modifier = local.luke.power.input.Bindings.down(Keys.MODIFIER), picker = local.luke.power.input.Bindings.down(Keys.PICKER);
    if (modifier && !modifierDown) debugBefore = mc.options.debugHud;
    if (active(mc) && Config.current().modePicker && modifier && picker && !pickerDown) {
      mc.options.debugHud = debugBefore;
      mc.openScreen(new ModePickerScreen());
    }
    modifierDown = modifier;
    pickerDown = picker;
    if (speedTicks > 0) speedTicks--;
    FlightController.tick(mc);
  }

  public static void showSpeed() {
    speedTicks = 40;
  }

  public static String speedText() {
    return speedTicks > 0
            && Config.current().showSpectatorSpeed
            && local(minecraft)
            && Modes.spectator(minecraft.player)
        ? "Spectator speed: "
            + Math.round(((ModePlayer) minecraft.player).power_spectatorSpeed() / .05f * 100)
            + "%"
        : null;
  }
}
