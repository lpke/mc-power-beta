package local.luke.power.map;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

/** Shared map/world symbols, including the reference nine-pixel deathpoint glyph. */
public final class WaypointSymbols {
  private WaypointSymbols() {}
  public static int color(Waypoint point, MapSettings settings) {
    return MinimapArrows.destination(point) ? settings.destinationArrowColor : point.colour;
  }
  public static double opacity(Waypoint point, MapSettings settings) {
    return MinimapArrows.destination(point) ? settings.destinationArrowOpacity : 1;
  }
  public static void draw(Minecraft mc, Waypoint point, double x, double y, double radius,
      int color, double opacity) {
    if (point.death) skull(mc, x, y, radius * 2 / 9, opacity);
    else diamond(x, y, radius, color, opacity);
  }
  public static void diamond(double x, double y, double size, int color, double opacity) {
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    double r = size + .8;
    GL11.glColor4d(0, 0, 0, opacity);
    GL11.glBegin(GL11.GL_QUADS);
    GL11.glVertex2d(x, y - r); GL11.glVertex2d(x + r, y);
    GL11.glVertex2d(x, y + r); GL11.glVertex2d(x - r, y);
    GL11.glEnd();
    GL11.glBegin(GL11.GL_TRIANGLES);
    for (int facet = 0; facet < 4; facet++) {
      int rgb = facetColor(color, facet);
      GL11.glColor4d((rgb >> 16 & 255) / 255d, (rgb >> 8 & 255) / 255d, (rgb & 255) / 255d, opacity);
      GL11.glVertex2d(x, y);
      vertex(x, y, size, facet);
      vertex(x, y, size, (facet + 1) & 3);
    }
    GL11.glEnd();
    GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glColor4f(1, 1, 1, 1);
  }
  private static void vertex(double x, double y, double radius, int corner) {
    GL11.glVertex2d(x + (corner == 1 ? radius : corner == 3 ? -radius : 0),
        y + (corner == 2 ? radius : corner == 0 ? -radius : 0));
  }
  static int facetColor(int color, int facet) {
    return switch (facet) {
      case 0 -> MapMath.shade(color, 1.08);
      case 1 -> MapMath.shade(color, .66);
      case 2 -> MapMath.shade(color, .84);
      default -> MapMath.mix(color, 0xffffff, .28);
    };
  }
  public static void skull(Minecraft mc, double x, double y, double scale, double opacity) {
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, mc.textureManager.getTextureId("/assets/powerbeta/map/deathpoint.png"));
    double r = 4.5 * scale;
    double offset = Math.max(.5, scale);
    GL11.glColor4d(0, 0, 0, opacity * .65);
    skullQuad(x + offset, y + offset, r);
    GL11.glColor4d(1, 1, 1, opacity);
    skullQuad(x, y, r);
    GL11.glColor4f(1, 1, 1, 1);
  }
  private static void skullQuad(double x, double y, double r) {
    GL11.glBegin(GL11.GL_QUADS);
    GL11.glTexCoord2f(0, 0); GL11.glVertex2d(x - r, y - r);
    GL11.glTexCoord2f(1, 0); GL11.glVertex2d(x + r, y - r);
    GL11.glTexCoord2f(1, 1); GL11.glVertex2d(x + r, y + r);
    GL11.glTexCoord2f(0, 1); GL11.glVertex2d(x - r, y + r);
    GL11.glEnd();
  }
}
