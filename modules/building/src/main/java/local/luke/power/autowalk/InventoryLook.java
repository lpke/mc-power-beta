package local.luke.power.autowalk;

import local.luke.power.building.config.Config;
import local.luke.power.input.InventoryGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Mouse;

/** Owns ordered GUI mouse events; the renderer owns sensitivity and camera rotation. */
public final class InventoryLook {
  private static final InventoryLookDrag DRAG = new InventoryLookDrag();
  private static final InventoryPointer POINTER = new InventoryPointer();
  private static boolean captured, previouslyGrabbed, events, capturing;
  private static Object motionScreen, motionWorld, motionPlayer;
  private InventoryLook() {}

  public static boolean outsideGui(Minecraft mc, Screen screen, int x, int y) {
    return Config.current().inventoryMouseLook && InventoryMovement.allows(mc, screen)
        && screen instanceof InventoryGui gui && !gui.power$containsGuiSpace(x, y);
  }

  public static boolean active() { return DRAG.active(); }
  public static boolean capturing() { return capturing; }
  public static boolean hasMotion() { return POINTER.hasMotion(); }
  public static int eventX(int nativeX) { return events ? POINTER.x() : nativeX; }
  public static int eventY(int nativeY) { return events ? POINTER.y() : nativeY; }

  /** Called for every event, before the normal screen handler, without changing the native queue. */
  public static boolean mouseEvent(Minecraft mc, Screen screen) {
    if (!Config.current().inventoryMouseLook || !InventoryMovement.allows(mc, screen)) return active();
    events = true;
    POINTER.event(Mouse.getEventX(), Mouse.getEventY(), Mouse.getEventDX(), Mouse.getEventDY(), Mouse.isGrabbed());
    boolean consumed = active();
    if (Mouse.getEventButton() != 1) return consumed;
    if (!Mouse.getEventButtonState()) {
      if (active()) { DRAG.stop(); POINTER.release(); }
      return consumed;
    }
    if (active()) return true;
    int x = POINTER.x() * screen.width / mc.displayWidth;
    int y = screen.height - POINTER.y() * screen.height / mc.displayHeight - 1;
    if (!DRAG.press(screen, mc.world, mc.player, mc.currentScreen == screen && outsideGui(mc, screen, x, y)))
      return false;
    POINTER.press();
    motionScreen = screen; motionWorld = mc.world; motionPlayer = mc.player;
    return true;
  }

  /** Grab/ungrab clears LWJGL's queues, so it must happen after the whole mouse batch. */
  public static void finishEvents() {
    if (!events) return;
    events = false;
    if (active() && !captured && Mouse.isButtonDown(1)) {
      captureMouse();
    } else if (!active() && (captured || POINTER.restore())) {
      // Xwayland can ignore visible-cursor warps. Even a complete quick gesture needs
      // the native hidden-cursor transition, after all queued clicks are dispatched.
      if (!captured && !Mouse.isGrabbed()) captureMouse();
      restoreCursor();
    }
    POINTER.batchFinished(captured);
  }

  private static void captureMouse() {
    previouslyGrabbed = Mouse.isGrabbed();
    capturing = true;
    try { Mouse.setGrabbed(true); }
    finally { capturing = false; }
    captured = true;
  }

  /** Ordered events supply motion once, including complete clicks between two renders. */
  public static void supplyMotion(Minecraft mc) {
    if (!active() && !hasMotion()) return;
    mc.field_2767.field_2586 = POINTER.takeX();
    mc.field_2767.field_2587 = POINTER.takeY();
  }

  private static void restoreCursor() {
    if (!captured || !previouslyGrabbed) Mouse.setCursorPosition(POINTER.x(), POINTER.y());
    if (captured) Mouse.setGrabbed(previouslyGrabbed);
    captured = false;
    Mouse.getDX(); Mouse.getDY();
  }

  public static void stop() {
    if (Mouse.isCreated() && (active() || captured || POINTER.restore())) restoreCursor();
    DRAG.stop(); POINTER.clear(); events = false;
    motionScreen = motionWorld = motionPlayer = null;
  }

  public static void update(Minecraft mc) {
    Screen screen = mc.currentScreen;
    boolean eligible = Config.current().inventoryMouseLook && InventoryMovement.allows(mc, screen);
    if ((active() || hasMotion()) && (!eligible || motionScreen != screen || motionWorld != mc.world || motionPlayer != mc.player))
      stop();
    if (eligible) {
      // Display.update polls after the game tick. Dispatch that new mouse batch before
      // rendering too, for smooth looking and to avoid capturing over an unread release.
      // Keyboard input stays in the normal tick path. All clicks use the normal handler.
      while (mc.currentScreen == screen && Mouse.next()) screen.onMouseEvent();
      finishEvents();
    }
    // Discard polling deltas: they include cursor warps and movement outside the drag.
    if (!mc.field_2778 && screen != null) mc.field_2767.method_1972();
  }
}
