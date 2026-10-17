package local.luke.power.ui;

import java.util.List;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

/** Preview and gameplay share the same line placement, scaling and native font. */
public final class DebugTextRenderer {
  private DebugTextRenderer() {}
  public static void draw(Minecraft mc, List<DebugOverlay.Line> lines, double size, int color) {
    for (var line : lines) {
      if (line.id().isEmpty() || size == 1) mc.textRenderer.drawWithShadow(line.text(), line.x(), line.y(), color);
      else {
        GL11.glPushMatrix();
        try {
          GL11.glTranslated(line.x(), line.y(), 0); GL11.glScaled(size, size, 1);
          mc.textRenderer.drawWithShadow(line.text(), 0, 0, color);
        } finally { GL11.glPopMatrix(); }
      }
    }
  }
}
