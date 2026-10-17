package local.luke.power.hud;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

/** One renderer for live clusters and their editor preview. */
public final class InfoHudRenderer {
  private InfoHudRenderer() {}
  public static void draw(Minecraft mc, InfoHudFrame frame) {
    GL11.glPushMatrix();
    try {
      GL11.glTranslated(frame.layout().x(), frame.layout().y(), 0);
      GL11.glScaled(frame.scale(), frame.scale(), 1);
      for (int row = 0; row < frame.lines().size(); row++) {
        String line = frame.lines().get(row);
        mc.textRenderer.drawWithShadow(line, frame.layout().lineX(frame.textWidth(), mc.textRenderer.getWidth(line)), row * 10, frame.argb());
      }
    } finally { GL11.glPopMatrix(); }
  }
}
