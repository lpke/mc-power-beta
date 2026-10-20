package local.luke.power.ui;

import org.lwjgl.opengl.GL11;

/** Modern debug crosshair: ten GUI units, four/two physical-pixel black/color strokes. */
public final class DebugAxes {
  private static final int[] COLORS = {0xFF0000, 0x00FF00, 0x7F7FFF};
  private DebugAxes() {}
  public static double[] endpoint(int axis, double yaw, double pitch) {
    double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
    return switch (axis) {
      case 0 -> new double[]{-10 * Math.cos(y), 10 * Math.sin(y) * Math.sin(p)};
      case 1 -> new double[]{0, -10 * Math.cos(p)};
      case 2 -> new double[]{-10 * Math.sin(y), -10 * Math.cos(y) * Math.sin(p)};
      default -> throw new IllegalArgumentException("Invalid debug axis");
    };
  }
  public static void draw(double x, double y, double yaw, double pitch, double pixelsPerUnit) {
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    GL11.glPushMatrix();
    try {
      GL11.glTranslated(x, y, 0);
      GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_FOG); GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_CULL_FACE);
      GL11.glDisable(GL11.GL_BLEND); GL11.glDepthMask(false);
      for (int pass = 0; pass < 2; pass++) {
        double half = (pass == 0 ? 2 : 1) / Math.max(.01, pixelsPerUnit);
        GL11.glBegin(GL11.GL_QUADS);
        for (int axis = 0; axis < 3; axis++) {
          double[] end = endpoint(axis, yaw, pitch);
          double length = Math.hypot(end[0], end[1]);
          if (length < 1e-6) continue;
          double dx = -end[1] * half / length, dy = end[0] * half / length;
          int color = pass == 0 ? 0 : COLORS[axis];
          GL11.glColor3ub((byte)(color >> 16), (byte)(color >> 8), (byte)color);
          GL11.glVertex2d(dx, dy); GL11.glVertex2d(end[0] + dx, end[1] + dy);
          GL11.glVertex2d(end[0] - dx, end[1] - dy); GL11.glVertex2d(-dx, -dy);
        }
        GL11.glEnd();
      }
    } finally { GL11.glPopMatrix(); GL11.glPopAttrib(); }
  }
}
