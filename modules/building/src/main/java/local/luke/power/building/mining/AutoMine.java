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
    if (client != mc) { client = mc; AttackInput.register(AutoMine::active); }
    if (world != mc.world || player != mc.player) {
      stop(); world = mc.world; player = mc.player;
    }
    boolean matches = Bindings.down(KEY);
    TOGGLE.update(Bindings.physical(KEY.code), eligible(), matches);
  }
  private static boolean eligible() {
    return client != null && client.world != null && client.player != null
        && client.world == world && client.player == player
        && client.player.health > 0 && !client.player.dead
        && client.currentScreen == null && client.field_2778 && Display.isActive()
        && Config.current().autoMine && !InteractionState.carryingContainer
        && !MovementOwnership.cameraControlsMovement();
  }
  public static boolean active() {
    if (!eligible()) stop();
    return TOGGLE.active();
  }
  public static void stop() { TOGGLE.stop(); }
}
