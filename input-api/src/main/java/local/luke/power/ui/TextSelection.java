package local.luke.power.ui;

import org.lwjgl.opengl.GL11;

/** Both chat surfaces paint the same translucent selection without changing font GL state. */
public final class TextSelection {
  public static final int COLOR = 0x80345c91;
  private TextSelection() {}
  public static void background(int left, int top, int right, int bottom) {
    if (right <= left || bottom <= top) return;
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);
    try {
      GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glColor4ub((byte)(COLOR >> 16), (byte)(COLOR >> 8), (byte)COLOR, (byte)(COLOR >>> 24));
      GL11.glBegin(GL11.GL_QUADS);
      GL11.glVertex2i(left, bottom); GL11.glVertex2i(right, bottom);
      GL11.glVertex2i(right, top); GL11.glVertex2i(left, top);
      GL11.glEnd();
    } finally { GL11.glPopAttrib(); }
  }
}
