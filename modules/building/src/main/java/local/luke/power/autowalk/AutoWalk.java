package local.luke.power.autowalk;

import net.minecraft.client.Minecraft;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.input.Keyboard;
import local.luke.power.input.Bindings;
import local.luke.power.input.MovementScreens;
import org.lwjgl.opengl.Display;

public final class AutoWalk {
  public static final KeyBinding KEY = new KeyBinding("Auto-walk (toggle)", Keyboard.KEY_NONE);
  public static final KeyBinding LINE_LOCK_KEY = new KeyBinding("Auto-walk line lock (toggle)", Keyboard.KEY_NONE);
  private static final WalkToggle TOGGLE = new WalkToggle();
  private static final LineLock LINE_LOCK = new LineLock();
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
      local.luke.power.input.MovementOwnership.registerAutoWalkLineLock(AutoWalk::lineLocked);
      Bindings.onKeyPress("building.autoWalk", AutoWalk::press);
      Bindings.onMousePress("building.autoWalk", AutoWalk::press);
      Bindings.onRelease("building.autoWalk", AutoWalk::release);
      Bindings.onKeyPress("building.lineLock", AutoWalk::toggleLineLock);
      Bindings.onMousePress("building.lineLock", AutoWalk::toggleLineLock);
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
    TOGGLE.poll(inventory ? MovementScreens.keyboardDown(KEY) : Bindings.down(KEY), canWalk(), settings.autoWalkHoldToWalk,
        System.nanoTime(), settings.autoWalkHoldMillis);
    if (!TOGGLE.isActive()) LINE_LOCK.stop();
    forwardWasDown = forward;
    backWasDown = back;
  }

  private static void press(int key) {
    if (client == null || key != KEY.code || !Bindings.matches(KEY, key) || !Display.isActive()
        || client.currentScreen != null && !InventoryMovement.eligible(client, client.currentScreen)) return;
    boolean inventory = InventoryMovement.allows(client, client.currentScreen);
    if (inventory && !MovementScreens.movementKey(key)) return;
    var settings = local.luke.power.building.config.Config.current();
    boolean backwards = !local.luke.power.input.MovementOwnership.cameraControlsMovement()
        && Bindings.down(client.options.backKey) && !Bindings.down(client.options.forwardKey);
    TOGGLE.press(canWalk(), settings.autoWalkHoldToWalk, System.nanoTime(), backwards);
    if (!TOGGLE.isActive()) LINE_LOCK.stop();
  }

  private static void release(int key) {
    if (key != KEY.code) return;
    var settings = local.luke.power.building.config.Config.current();
    if (!settings.autoWalkCancelOnLostFocus && (!Display.isActive()
        || focusPause != null && client.currentScreen == focusPause)) TOGGLE.unfocused();
    TOGGLE.poll(false, canWalk(), settings.autoWalkHoldToWalk, System.nanoTime(), settings.autoWalkHoldMillis);
  }

  private static boolean canWalk() {
    return client != null
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
    TOGGLE.cancel();
    LINE_LOCK.stop();
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
  private static void toggleLineLock(int key) {
    if (!Bindings.matches(LINE_LOCK_KEY,key) || !canWalk() || !TOGGLE.isActive() || client.world.isRemote) return;
    if (LINE_LOCK.active()) LINE_LOCK.stop();
    else LINE_LOCK.start(client.player.x,client.player.z,client.player.yaw);
  }
  public static boolean lineLocked() { return isWalking() && LINE_LOCK.active(); }
  public static double lockX(double x, double halfWidth, double delta) { return lineLocked() ? LINE_LOCK.x(x, halfWidth, delta) : delta; }
  public static double lockZ(double z, double halfWidth, double delta) { return lineLocked() ? LINE_LOCK.z(z, halfWidth, delta) : delta; }

  public static float movementInput(float vanillaInput, boolean forwardHeld, boolean backHeld, boolean sneaking) {
    return TOGGLE.movementInput(vanillaInput, forwardHeld, backHeld, sneaking);
  }
}
