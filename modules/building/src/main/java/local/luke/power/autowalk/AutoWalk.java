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
  private static boolean forwardWasDown, backWasDown;

  private AutoWalk() {}

  public static void tick(Minecraft minecraft) {
    local.luke.power.building.BuildingIndicators.register(minecraft);
    if (client != minecraft) {
      client = minecraft;
      local.luke.power.input.MovementOwnership.registerAutoWalk(AutoWalk::isWalking);
      local.luke.power.input.MovementOwnership.registerAutoWalkInput(AutoWalk::input);
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
    boolean forward = inventory ? MovementScreens.keyboardDown(minecraft.options.forwardKey) : Bindings.down(minecraft.options.forwardKey);
    boolean back = inventory ? MovementScreens.keyboardDown(minecraft.options.backKey) : Bindings.down(minecraft.options.backKey);
    if (inventory && !local.luke.power.input.MovementOwnership.cameraControlsMovement()
        && (forward && !forwardWasDown || back && !backWasDown)) TOGGLE.stop();
    TOGGLE.update(inventory ? MovementScreens.keyboardDown(KEY) : Bindings.down(KEY), canWalk(), settings.autoWalkHoldToWalk,
        System.nanoTime(), settings.autoWalkHoldMillis,
        !local.luke.power.input.MovementOwnership.cameraControlsMovement()
            && back && !forward);
    forwardWasDown = forward;
    backWasDown = back;
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

  public static float input() { return isWalking() ? TOGGLE.input() : 0; }

  public static float movementInput(float vanillaInput, boolean forwardHeld, boolean backHeld, boolean sneaking) {
    return TOGGLE.movementInput(vanillaInput, forwardHeld, backHeld, sneaking);
  }
}
