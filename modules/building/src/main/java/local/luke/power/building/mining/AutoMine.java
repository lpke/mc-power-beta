package local.luke.power.building.mining;

import local.luke.power.building.config.Config;
import local.luke.power.input.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.opengl.Display;

public final class AutoMine {
  public static final KeyBinding KEY = new KeyBinding("Auto-mine (toggle)", 0);
  private static final AutoMineToggle TOGGLE = new AutoMineToggle();
  private static Minecraft client;
  private static Object world, player;
  private AutoMine() {}

  public static void tick(Minecraft mc) {
    if (client != mc) {
      client = mc; AttackInput.register(AutoMine::active);
      Bindings.onKeyPress("building.autoMine", AutoMine::press);
      Bindings.onMousePress("building.autoMine", AutoMine::press);
      Bindings.onRelease("building.autoMine", key -> { if (key == KEY.code) TOGGLE.release(); });
    }
    if (world != mc.world || player != mc.player) {
      stop(); world = mc.world; player = mc.player;
    }
    TOGGLE.poll(Bindings.physical(KEY.code), eligible());
  }
  private static void press(int key) {
    if (key == KEY.code) TOGGLE.press(eligible(), Bindings.matches(KEY, key));
  }
  private static boolean eligible() {
    return client != null && client.world != null && client.player != null
        && client.world == world && client.player == player
        && client.player.health > 0 && !client.player.dead
        && client.currentScreen == null && client.field_2778 && Display.isActive()
        && !InteractionState.carryingContainer
        && !MovementOwnership.cameraControlsMovement();
  }
  public static boolean active() {
    if (!eligible()) stop();
    return TOGGLE.active();
  }
  public static void stop() { TOGGLE.stop(); }
}
