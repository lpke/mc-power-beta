package local.luke.power.autowalk;

import local.luke.power.building.config.Config;
import local.luke.power.input.InventoryGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Mouse;

/** Keeps the inventory cursor free; the renderer still owns sensitivity and camera rotation. */
public final class InventoryLook {
  private static Screen owner;
  private static boolean active;
  private InventoryLook() {}

  public static boolean outsideGui(Minecraft mc, Screen screen, int x, int y) {
    return Config.current().inventoryMouseLook && InventoryMovement.allows(mc, screen)
        && screen instanceof InventoryGui gui && !gui.power$containsGuiSpace(x, y);
  }

  public static boolean active() { return active; }

  public static void update(Minecraft mc) {
    Screen screen = mc.currentScreen;
    boolean wanted = false;
    if (screen != null && mc.displayWidth > 0 && mc.displayHeight > 0) {
      int x = Mouse.getX() * screen.width / mc.displayWidth;
      int y = screen.height - Mouse.getY() * screen.height / mc.displayHeight - 1;
      wanted = Mouse.isButtonDown(1) && outsideGui(mc, screen, x, y);
    }
    // Discard idle movement and the first frame after a screen/gesture change.
    if (!mc.field_2778 && screen != null && (!wanted || !active || owner != screen))
      mc.field_2767.method_1972();
    active = wanted;
    owner = wanted ? screen : null;
  }
}
