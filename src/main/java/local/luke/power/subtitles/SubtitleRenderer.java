package local.luke.power.subtitles;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public final class SubtitleRenderer extends net.minecraft.client.gui.DrawableHelper {
  private static final SubtitleRenderer DRAW = new SubtitleRenderer();
  private SubtitleRenderer() {}
  public static void draw(Minecraft mc, SubtitleLayout layout) {
    if (layout.lines().isEmpty()) return;
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glPushMatrix();
    try {
      GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA); GL11.glAlphaFunc(GL11.GL_GREATER, 0);
      GL11.glTranslated(layout.x(), layout.y(), 0); GL11.glScaled(layout.scale(), layout.scale(), 1);
      int width = layout.halfWidth() * 2 + 2;
      for (var line : layout.lines()) {
        DRAW.fill(0, line.textY() - 1, width, line.textY() + 9, layout.background());
        int rgb = line.brightness() * 0x010101;
        mc.textRenderer.drawWithShadow(line.text(), line.textX(), line.textY(), rgb);
        if (line.arrow() == '<') mc.textRenderer.drawWithShadow("<", 1, line.textY(), rgb);
        else if (line.arrow() == '>') mc.textRenderer.drawWithShadow(">", width - 1 - mc.textRenderer.getWidth(">"), line.textY(), rgb);
      }
    } finally { GL11.glPopMatrix(); GL11.glPopAttrib(); }
  }
}
