package local.luke.power.autowalk;

import local.luke.power.building.config.Config;
import local.luke.power.input.InventoryGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Mouse;

/** Owns one right-click drag; the renderer still owns sensitivity and camera rotation. */
public final class InventoryLook {
  private static final InventoryLookDrag DRAG = new InventoryLookDrag();
  private static boolean ownsMouse, captured, previouslyGrabbed;
  private static int cursorX, cursorY;
  private static int pendingX, pendingY;
  private static Object motionScreen, motionWorld, motionPlayer;
  private InventoryLook() {}

  public static boolean outsideGui(Minecraft mc, Screen screen, int x, int y) {
    return Config.current().inventoryMouseLook && InventoryMovement.allows(mc, screen)
        && screen instanceof InventoryGui gui && !gui.power$containsGuiSpace(x, y);
  }

  public static boolean active() { return DRAG.active(); }

  public static boolean press(Minecraft mc, Screen screen, int x, int y) {
    int pressX = Mouse.getEventX(), pressY = Mouse.getEventY();
    stop();
    if (!DRAG.press(screen, mc.world, mc.player, mc.currentScreen == screen && outsideGui(mc, screen, x, y)))
      return false;
    cursorX = pressX; cursorY = pressY;
    previouslyGrabbed = Mouse.isGrabbed();
    ownsMouse = true;
    // Grabbing clears LWJGL's event queue. Wait until it has dispatched this batch:
    // a quick press/move/release must keep its release coordinates and never grab.
    return true;
  }

  public static void release(Minecraft mc) {
    finish(mc, Mouse.getEventX(), Mouse.getEventY());
  }

  private static void finish(Minecraft mc, int x, int y) {
    if (!active()) return;
    int dx = captured ? Mouse.getDX() : x - cursorX;
    int dy = captured ? Mouse.getDY() : y - cursorY;
    stop();
    if (Config.current().inventoryMouseLook && InventoryMovement.allows(mc, mc.currentScreen))
      queueMotion(mc, dx, dy);
  }

  private static void queueMotion(Minecraft mc, int dx, int dy) {
    pendingX = dx; pendingY = dy;
    motionScreen = mc.currentScreen; motionWorld = mc.world; motionPlayer = mc.player;
  }

  public static boolean hasMotion() { return pendingX != 0 || pendingY != 0; }

  /** Feed completed quick gestures through the renderer's native sensitivity and rotation. */
  public static void supplyMotion(Minecraft mc) {
    if (!hasMotion()) return;
    mc.field_2767.field_2586 = pendingX; mc.field_2767.field_2587 = pendingY;
    clearMotion();
  }

  private static void clearMotion() {
    pendingX = pendingY = 0;
    motionScreen = motionWorld = motionPlayer = null;
  }

  public static void stop() {
    DRAG.stop();
    clearMotion();
    if (!ownsMouse) return;
    ownsMouse = false;
    boolean wasCaptured = captured; captured = false;
    if (!Mouse.isCreated()) return;
    // For a captured drag, set the ungrab destination before the single native warp.
    // A completed quick gesture uses a single absolute warp without changing grab state.
    if (!previouslyGrabbed) Mouse.setCursorPosition(cursorX, cursorY);
    if (wasCaptured) Mouse.setGrabbed(previouslyGrabbed);
    Mouse.getDX(); Mouse.getDY();
  }

  public static void update(Minecraft mc) {
    Screen screen = mc.currentScreen;
    boolean eligible = Config.current().inventoryMouseLook && InventoryMovement.allows(mc, screen);
    if (hasMotion() && (!eligible || motionScreen != screen || motionWorld != mc.world || motionPlayer != mc.player))
      clearMotion();
    if (active()) {
      if (!DRAG.update(screen, mc.world, mc.player, true, eligible)) stop();
      else if (!Mouse.isButtonDown(1)) finish(mc, Mouse.getX(), Mouse.getY());
      else if (!captured) {
        queueMotion(mc, Mouse.getX() - cursorX, Mouse.getY() - cursorY);
        mc.field_2767.method_1972();
        Mouse.setGrabbed(true);
        captured = true;
      }
    }
    // Idle movement and cursor restoration must not turn into camera movement.
    if (!mc.field_2778 && screen != null && !active())
      mc.field_2767.method_1972();
  }
}
