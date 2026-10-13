package local.luke.power.autowalk;

import local.luke.power.building.config.Config;
import local.luke.power.input.InventoryGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Mouse;

/** Owns one right-click drag; the renderer still owns sensitivity and camera rotation. */
public final class InventoryLook {
  private static final InventoryLookDrag DRAG = new InventoryLookDrag();
  private static boolean ownsMouse, previouslyGrabbed;
  private static int cursorX, cursorY;
  private InventoryLook() {}

  public static boolean outsideGui(Minecraft mc, Screen screen, int x, int y) {
    return Config.current().inventoryMouseLook && InventoryMovement.allows(mc, screen)
        && screen instanceof InventoryGui gui && !gui.power$containsGuiSpace(x, y);
  }

  public static boolean active() { return DRAG.active(); }

  public static boolean press(Minecraft mc, Screen screen, int x, int y) {
    stop();
    if (!DRAG.press(screen, mc.world, mc.player, mc.currentScreen == screen && outsideGui(mc, screen, x, y)))
      return false;
    cursorX = Mouse.getEventX(); cursorY = Mouse.getEventY();
    previouslyGrabbed = Mouse.isGrabbed();
    mc.field_2767.method_1972();
    Mouse.setGrabbed(true);
    ownsMouse = true;
    return true;
  }

  public static void stop() {
    DRAG.stop();
    if (!ownsMouse) return;
    ownsMouse = false;
    if (!Mouse.isCreated()) return;
    // While grabbed, this sets LWJGL's ungrab destination. Ungrabbing then warps,
    // polls and clears the old relative events in one operation. Warping afterwards
    // races the first warp and can restore the last polled position instead of the press.
    if (!previouslyGrabbed && Mouse.isGrabbed()) Mouse.setCursorPosition(cursorX, cursorY);
    Mouse.setGrabbed(previouslyGrabbed);
    Mouse.getDX(); Mouse.getDY();
  }

  public static void update(Minecraft mc) {
    Screen screen = mc.currentScreen;
    if (!DRAG.update(screen, mc.world, mc.player, Mouse.isButtonDown(1),
        Config.current().inventoryMouseLook && InventoryMovement.allows(mc, screen))) stop();
    // Idle movement and cursor restoration must not turn into camera movement.
    if (!mc.field_2778 && screen != null && !active())
      mc.field_2767.method_1972();
  }
}
