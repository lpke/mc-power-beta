package local.luke.power.ui;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

/** A bundled renderer capture, cropped to fill without stretching or loading a world. */
final class HudPreviewBackground {
  static final String TEXTURE = "/assets/powerbeta/textures/hud-preview-forest.png";
  private HudPreviewBackground() {}
  static void prepare(Minecraft mc) { mc.textureManager.getTextureId(TEXTURE); }
  static void draw(Minecraft mc, int width, int height) {
    double aspect = width / (double)height, imageAspect = 16d / 9;
    double u = Math.min(1, aspect / imageAspect), v = Math.min(1, imageAspect / aspect);
    double u0 = (1 - u) / 2, v0 = (1 - v) / 2;
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, mc.textureManager.getTextureId(TEXTURE));
    GL11.glColor4f(1, 1, 1, 1);
    GL11.glBegin(GL11.GL_QUADS);
    GL11.glTexCoord2d(u0, v0 + v); GL11.glVertex2d(0, height);
    GL11.glTexCoord2d(u0 + u, v0 + v); GL11.glVertex2d(width, height);
    GL11.glTexCoord2d(u0 + u, v0); GL11.glVertex2d(width, 0);
    GL11.glTexCoord2d(u0, v0); GL11.glVertex2d(0, 0);
    GL11.glEnd();
  }
}
