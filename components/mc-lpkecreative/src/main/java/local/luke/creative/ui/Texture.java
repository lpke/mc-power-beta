package local.luke.creative.ui;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public final class Texture {
  private Texture() {}

  public static void draw(
      Minecraft mc,
      String name,
      int x,
      int y,
      int w,
      int h,
      int u,
      int v,
      int regionW,
      int regionH,
      int textureW,
      int textureH) {
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glBindTexture(
        GL11.GL_TEXTURE_2D,
        mc.textureManager.getTextureId("/assets/lpkecreative/textures/" + name + ".png"));
    GL11.glColor4f(1, 1, 1, 1);
    double u0 = (double) u / textureW, u1 = (double) (u + regionW) / textureW;
    double v0 = (double) v / textureH, v1 = (double) (v + regionH) / textureH;
    GL11.glBegin(GL11.GL_QUADS);
    GL11.glTexCoord2d(u0, v1);
    GL11.glVertex3d(x, y + h, 0);
    GL11.glTexCoord2d(u1, v1);
    GL11.glVertex3d(x + w, y + h, 0);
    GL11.glTexCoord2d(u1, v0);
    GL11.glVertex3d(x + w, y, 0);
    GL11.glTexCoord2d(u0, v0);
    GL11.glVertex3d(x, y, 0);
    GL11.glEnd();
  }
}
