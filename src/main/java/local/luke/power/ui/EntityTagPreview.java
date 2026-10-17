package local.luke.power.ui;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

/** A live native-font ID tag follows the mob's head in the cached forest capture. */
final class EntityTagPreview {
  // Capture coordinates are in the original image, before the shared crop-to-fill.
  static final double HEAD_X = .4455689, HEAD_Y = .39660347;
  private static final double UNIT_X = .0022808076, UNIT_Y = .004054769;
  private EntityTagPreview() {}
  static void draw(UiScreen screen, Minecraft mc, int width, int height) {
    double aspect = width / (double)height, imageAspect = 16d / 9;
    double u = Math.min(1, aspect / imageAspect), v = Math.min(1, imageAspect / aspect);
    double x = (HEAD_X - (1-u)/2) / u * width, y = (HEAD_Y - (1-v)/2) / v * height;
    String id = "2"; int span = mc.textRenderer.getWidth(id);
    GL11.glPushMatrix();
    try {
      GL11.glTranslated(x, y, 0);
      GL11.glScaled(UNIT_X * width / u, UNIT_Y * height / v, 1);
      screen.rectangle(-span/2-1,-1,span-span/2+1,8,0x40000000);
      mc.textRenderer.draw(id,-span/2,0,0xffffff);
    } finally { GL11.glPopMatrix(); }
  }
}
