package local.luke.power.ui;

import local.luke.power.inventory.tools.InventoryToolsLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.opengl.GL11;

/** Small inventory controls keep the same neutral appearance under the pointer. */
public final class CompactInventoryButton extends Screen {
  private static final CompactInventoryButton DRAW = new CompactInventoryButton();
  private static final int FACE = 0xffaaaaaa, LIGHT = 0xffbcbcbc, SHADE = 0xff909090;
  private static final java.nio.DoubleBuffer CLIP = org.lwjgl.BufferUtils.createDoubleBuffer(4);
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
      int left = x + (size - mc.textRenderer.getWidth(label)) / 2, colour = enabled ? 0x606060 : 0x858585;
      // Keep native font pixels, omit one upper-stem row, and leave the bottom inset empty.
      GL11.glEnable(GL11.GL_CLIP_PLANE0); GL11.glEnable(GL11.GL_CLIP_PLANE1);
      clip(y + 2, y + 4); mc.textRenderer.draw(label, left, y + 2, colour);
      clip(y + 4, y + 8); mc.textRenderer.draw(label, left, y + 1, colour);
    } finally { GL11.glPopAttrib(); }
  }
  private static void clip(int top, int bottom) {
    CLIP.clear(); CLIP.put(new double[]{0, 1, 0, -top}).flip(); GL11.glClipPlane(GL11.GL_CLIP_PLANE0, CLIP);
    CLIP.clear(); CLIP.put(new double[]{0, -1, 0, bottom}).flip(); GL11.glClipPlane(GL11.GL_CLIP_PLANE1, CLIP);
  }
}
