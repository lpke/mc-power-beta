package local.luke.power.ui;

import local.luke.power.inventory.tools.InventoryToolsLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.opengl.GL11;

/** Small inventory controls keep the same neutral appearance under the pointer. */
public final class CompactInventoryButton extends Screen {
  private static final CompactInventoryButton DRAW = new CompactInventoryButton();
  private static final int FACE = 0xffaaaaaa, LIGHT = 0xffbcbcbc, SHADE = 0xff909090;
  private CompactInventoryButton() {}

  public static void draw(Minecraft mc, String label, int x, int y, boolean enabled) {
    int size = InventoryToolsLayout.BUTTON_SIZE;
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    try {
      GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_DEPTH_TEST);
      DRAW.fill(x, y, x + size, y + size, FACE);
      DRAW.fill(x, y, x + size - 1, y + 1, LIGHT);
      DRAW.fill(x, y + 1, x + 1, y + size - 1, LIGHT);
      DRAW.fill(x + size - 1, y, x + size, y + size, SHADE);
      DRAW.fill(x, y + size - 1, x + size - 1, y + size, SHADE);
      GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glColor4f(1, 1, 1, 1);
      mc.textRenderer.draw(label, x + (size - mc.textRenderer.getWidth(label)) / 2, y + (size - 8) / 2,
          enabled ? 0x606060 : 0x858585);
    } finally { GL11.glPopAttrib(); }
  }
}
