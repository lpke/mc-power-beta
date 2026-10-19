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
    for (int outline = 1; outline >= 0; outline--) {
      double r = size + outline * .8;
      int rgb = outline == 1 ? 0 : color;
      GL11.glColor4d((rgb >> 16 & 255) / 255d, (rgb >> 8 & 255) / 255d, (rgb & 255) / 255d, opacity);
      GL11.glBegin(GL11.GL_QUADS);
      GL11.glVertex2d(x, y - r); GL11.glVertex2d(x + r, y);
      GL11.glVertex2d(x, y + r); GL11.glVertex2d(x - r, y);
      GL11.glEnd();
    }
    GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glColor4f(1, 1, 1, 1);
  }
  public static void skull(Minecraft mc, double x, double y, double scale, double opacity) {
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, mc.textureManager.getTextureId("/assets/powerbeta/map/deathpoint.png"));
    GL11.glColor4d(1, 1, 1, opacity);
    double r = 4.5 * scale;
    GL11.glBegin(GL11.GL_QUADS);
    GL11.glTexCoord2f(0, 0); GL11.glVertex2d(x - r, y - r);
    GL11.glTexCoord2f(1, 0); GL11.glVertex2d(x + r, y - r);
    GL11.glTexCoord2f(1, 1); GL11.glVertex2d(x + r, y + r);
    GL11.glTexCoord2f(0, 1); GL11.glVertex2d(x - r, y + r);
    GL11.glEnd(); GL11.glColor4f(1, 1, 1, 1);
  }
}
