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
  private static boolean openingFocusPause;
  private static net.minecraft.client.gui.screen.Screen focusPause;

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
      stop();
      world = minecraft.world;
      player = minecraft.player;
    }
    var settings = local.luke.power.building.config.Config.current();
    if (focusPause != null && minecraft.currentScreen == focusPause && canWalk()) {
      TOGGLE.unfocused();
      return;
    }
    if (!Display.isActive() && !settings.autoWalkCancelOnLostFocus && canWalk()) {
      // Keep the activation direction, but discard a hold gesture and stale key edges.
      TOGGLE.unfocused();
      forwardWasDown = backWasDown = false;
      return;
    }
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
        && (client.currentScreen == null || client.currentScreen == focusPause || InventoryMovement.eligible(client, client.currentScreen))
        && (!local.luke.power.building.config.Config.current().autoWalkCancelOnLostFocus || Display.isActive());
  }

  public static boolean isWalking() {
    if (!canWalk()) {
      TOGGLE.stop();
    }
    return TOGGLE.isActive();
  }

  public static void stop() {
    TOGGLE.stop();
    focusPause = null;
  }

  public static void focusUpdated() {
    if (Display.isActive()) return;
    if (local.luke.power.building.config.Config.current().autoWalkCancelOnLostFocus) stop();
    else TOGGLE.unfocused();
    forwardWasDown = backWasDown = false;
  }

  public static void focusPause(Minecraft mc, Runnable pause) {
    openingFocusPause = mc.currentScreen == null && !local.luke.power.building.config.Config.current().autoWalkCancelOnLostFocus;
    try { pause.run(); }
    finally { openingFocusPause = false; }
  }

  public static void screenOpened(Minecraft mc, net.minecraft.client.gui.screen.Screen screen) {
    if (screen == null) { focusPause = null; return; }
    if ((openingFocusPause || screen == focusPause) && TOGGLE.isActive()) { focusPause = screen; return; }
    if (!InventoryMovement.eligible(mc, screen)) stop();
  }

  public static boolean suppliesMovement() {
    // Focus gates manual inventory keys, not the direction retained by auto-walk.
    return isWalking() && (client.currentScreen == null || InventoryMovement.eligible(client, client.currentScreen));
  }

  public static float input() { return suppliesMovement() ? TOGGLE.input() : 0; }

  public static float movementInput(float vanillaInput, boolean forwardHeld, boolean backHeld, boolean sneaking) {
    return TOGGLE.movementInput(vanillaInput, forwardHeld, backHeld, sneaking);
  }
}
