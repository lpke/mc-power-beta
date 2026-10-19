package local.luke.power.map;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

/** One cached mesh per shape; the native texture manager owns uploads and pack reloads. */
final class MinimapBorder {
  private record Mesh(int diameter, float[] vertices) {}
  private static Mesh square, circle;
  private MinimapBorder() {}

  static void draw(Minecraft mc, MinimapGeometry.Layout bounds, boolean circular) {
    Mesh mesh = circular ? circle : square;
    if (mesh == null || mesh.diameter != bounds.size()) {
      mesh = new Mesh(bounds.size(), MinimapBorderGeometry.create(bounds.size(), circular));
      if (circular) circle = mesh; else square = mesh;
    }
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glBindTexture(GL11.GL_TEXTURE_2D,
        mc.textureManager.getTextureId("/assets/powerbeta/map/minimap-frame.png"));
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glColor4f(1, 1, 1, 1);
    GL11.glBegin(GL11.GL_QUADS);
    for (int i = 0; i < mesh.vertices.length; i += 4) {
      GL11.glTexCoord2f(mesh.vertices[i + 2], mesh.vertices[i + 3]);
      GL11.glVertex2f(bounds.x() + mesh.vertices[i], bounds.y() + mesh.vertices[i + 1]);
    }
    GL11.glEnd();
  }
}
