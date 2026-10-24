package local.luke.power.ui;

import net.minecraft.class_583;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/** Native inventory lighting and tint, independent of the preceding screen's GL state. */
public final class GuiItems {
  private GuiItems() {}

  public static void draw(ItemRenderer renderer, Minecraft mc, ItemStack stack, int x, int y, boolean decorations) {
    draw(renderer, mc, stack, x, y, 1, decorations);
  }

  public static void draw(ItemRenderer renderer, Minecraft mc, ItemStack stack, int x, int y, float scale, boolean decorations) {
    if (stack == null) return;
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      // Match HandledScreen and the native Creative palette. A renderer can enable
      // lighting after a flat sprite without supplying these lights or materials.
      GL11.glPushMatrix();
      try { GL11.glRotatef(120, 1, 0, 0); class_583.method_1930(); }
      finally { GL11.glPopMatrix(); }
      GL11.glColor4f(1, 1, 1, 1);
      GL11.glEnable(GL12.GL_RESCALE_NORMAL); GL11.glEnable(GL11.GL_DEPTH_TEST);
      GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_FOG);
      GL11.glTranslatef(x, y, 0); GL11.glScalef(scale, scale, 1);
      renderer.method_1487(mc.textRenderer, mc.textureManager, stack, 0, 0);
      if (decorations) renderer.method_1488(mc.textRenderer, mc.textureManager, stack, 0, 0);
    } finally { GL11.glPopMatrix(); GL11.glPopAttrib(); }
  }

  public static void decorations(ItemRenderer renderer, Minecraft mc, ItemStack stack, int x, int y) {
    if (stack == null) return;
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    try { renderer.method_1488(mc.textRenderer, mc.textureManager, stack, x, y); }
    finally { GL11.glPopAttrib(); }
  }
}
