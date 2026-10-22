package local.luke.power.input;

import org.lwjgl.opengl.GL11;

/** Draws GUI texture regions without depending on a module's Minecraft mappings. */
public final class GuiSprite {
  private GuiSprite() {}

  public static void draw(int x, int y, int width, int height,
                          int u, int v, int sourceWidth, int sourceHeight, int textureWidth, int textureHeight) {
    double left = u / (double)textureWidth, right = (u + sourceWidth) / (double)textureWidth;
    double top = v / (double)textureHeight, bottom = (v + sourceHeight) / (double)textureHeight;
    GL11.glBegin(GL11.GL_QUADS);
    GL11.glTexCoord2d(left, bottom); GL11.glVertex3f(x, y + height, 0);
    GL11.glTexCoord2d(right, bottom); GL11.glVertex3f(x + width, y + height, 0);
    GL11.glTexCoord2d(right, top); GL11.glVertex3f(x + width, y, 0);
    GL11.glTexCoord2d(left, top); GL11.glVertex3f(x, y, 0);
    GL11.glEnd();
  }
}
