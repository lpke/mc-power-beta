package local.luke.power.ui;

import org.lwjgl.opengl.GL11;

/** Smooth circular indicator; its owner supplies the larger input bounds. */
final class RadioButton {
  private static final int SEGMENTS = 64;
  private static final double[] X = new double[SEGMENTS + 1], Y = new double[SEGMENTS + 1];
  static {
    for (int i = 0; i <= SEGMENTS; i++) {
      double angle = i * Math.PI * 2 / SEGMENTS;
      X[i] = Math.cos(angle); Y[i] = Math.sin(angle);
    }
  }
  private RadioButton() {}

  static void draw(double x, double y, boolean selected, int colour) {
    float red = (colour >> 16 & 255) / 255f, green = (colour >> 8 & 255) / 255f, blue = (colour & 255) / 255f;
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LIGHTING_BIT);
    try {
      GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_CULL_FACE); GL11.glDisable(GL11.GL_ALPHA_TEST);
      GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glShadeModel(GL11.GL_SMOOTH);
      band(x, y, 6, 0, 5.5, 1, red, green, blue);
      band(x, y, 5.5, 1, 4.5, 1, red, green, blue);
      band(x, y, 4.5, 1, 4, 0, red, green, blue);
      if (selected) {
        GL11.glColor4f(red, green, blue, 1);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2d(x, y);
        for (int i = 0; i <= SEGMENTS; i++) GL11.glVertex2d(x + X[i] * 1.5, y + Y[i] * 1.5);
        GL11.glEnd();
        band(x, y, 2, 0, 1.5, 1, red, green, blue);
      }
    } finally { GL11.glPopAttrib(); }
  }

  private static void band(double x, double y, double outer, float outerAlpha,
      double inner, float innerAlpha, float red, float green, float blue) {
    GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
    for (int i = 0; i <= SEGMENTS; i++) {
      GL11.glColor4f(red, green, blue, outerAlpha);
      GL11.glVertex2d(x + X[i] * outer, y + Y[i] * outer);
      GL11.glColor4f(red, green, blue, innerAlpha);
      GL11.glVertex2d(x + X[i] * inner, y + Y[i] * inner);
    }
    GL11.glEnd();
  }
}
