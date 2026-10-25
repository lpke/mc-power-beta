package local.luke.power.map;

import local.luke.power.input.RenderedTerrainBounds;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

/** Four live renderer-grid edges; independent of terrain tile and texture caches. */
public final class RenderedArea {
  private static final double[] CORNERS = new double[8], LINE = new double[4];
  private RenderedArea() {}

  static void draw(Minecraft mc, MapProjection view, int dimension, boolean circle, MapSettings settings) {
    if (!settings.renderedArea || mc.world == null || mc.world.dimension.id != dimension
        || !(mc.worldRenderer instanceof RenderedTerrainBounds bounds)) return;
    double minX = bounds.powerTerrain$minX(), minZ = bounds.powerTerrain$minZ();
    double maxX = bounds.powerTerrain$maxX(), maxZ = bounds.powerTerrain$maxZ();
    if (!(maxX > minX && maxZ > minZ)) return;
    view.screen(minX, minZ, CORNERS, 0); view.screen(maxX, minZ, CORNERS, 2);
    view.screen(maxX, maxZ, CORNERS, 4); view.screen(minX, maxZ, CORNERS, 6);
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LINE_BIT
        | GL11.GL_COLOR_BUFFER_BIT);
    try {
      GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glDisable(GL11.GL_LIGHTING); GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glLineWidth(1); GL11.glColor4d(1, .74, .14, settings.opacity);
      GL11.glBegin(GL11.GL_LINES);
      for (int edge = 0; edge < 4; edge++) {
        int a = edge * 2, b = (a + 2) % 8;
        if (!MapLine.clip(view, circle, CORNERS[a], CORNERS[a + 1], CORNERS[b], CORNERS[b + 1], LINE)) continue;
        GL11.glVertex2d(LINE[0], LINE[1]); GL11.glVertex2d(LINE[2], LINE[3]);
      }
      GL11.glEnd();
    } finally { GL11.glPopAttrib(); }
  }
}
