package local.luke.power.autowalk;

import net.minecraft.client.Minecraft;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.input.Keyboard;
import local.luke.power.input.Bindings;
import local.luke.power.input.MovementScreens;
import org.lwjgl.opengl.Display;

public final class AutoWalk {
  public static final KeyBinding KEY = new KeyBinding("Auto-walk (toggle)", Keyboard.KEY_R);
  private static final WalkToggle TOGGLE = new WalkToggle();
  private static Minecraft client;
  private static Object world;
  private static Object player;

  private AutoWalk() {}

  public static void tick(Minecraft minecraft) {
    local.luke.power.building.BuildingIndicators.register(minecraft);
    if (client != minecraft) {
      client = minecraft;
      local.luke.power.input.MovementOwnership.registerAutoWalk(AutoWalk::isWalking);
      MovementScreens.register(screen -> screen instanceof net.minecraft.client.gui.screen.Screen s
          && InventoryMovement.allows(minecraft, s));
    }
    if (world != minecraft.world || player != minecraft.player) {
      TOGGLE.stop();
      world = minecraft.world;
      player = minecraft.player;
    }
    var settings = local.luke.power.building.config.Config.current();
    boolean inventory = InventoryMovement.allows(minecraft, minecraft.currentScreen);
    if (inventory && !local.luke.power.input.MovementOwnership.cameraControlsMovement()
        && (MovementScreens.keyboardDown(minecraft.options.forwardKey)
        || MovementScreens.keyboardDown(minecraft.options.backKey))) TOGGLE.stop();
    TOGGLE.update(inventory ? MovementScreens.keyboardDown(KEY) : Bindings.down(KEY), canWalk(), settings.autoWalkHoldToWalk,
        System.nanoTime(), settings.autoWalkHoldMillis);
  }

  private static boolean canWalk() {
    return local.luke.power.building.config.Config.current().autoWalk
        && client != null
        && client.world != null
        && client.player != null
        && client.player.health > 0 && !client.player.dead
        && (client.currentScreen == null || InventoryMovement.allows(client, client.currentScreen))
        && Display.isActive();
  }

  public static boolean isWalking() {
    if (!canWalk()) {
      TOGGLE.stop();
    }
    return TOGGLE.isActive();
  }

  public static void stop() {
    TOGGLE.stop();
  }
}
