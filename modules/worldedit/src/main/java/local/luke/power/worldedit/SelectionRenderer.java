package local.luke.power.worldedit;

import local.luke.power.worldedit.core.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public final class SelectionRenderer {
  private static final int[] COLORS = {0x66DDFF, 0x66FF88, 0xFFA040, 0xDD88FF};

  public static void render(Minecraft mc, float delta) {
    var e = WorldEditor.current();
    var s = WorldEditor.settings();
    if (!s.showSelection
        || e == null
        || e.pos1 == null && e.pos2 == null
        || !WorldEditor.available(mc)) return;
    var player = mc.field_2807;
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glPushMatrix();
    try {
      GL11.glTranslated(
          -(player.field_1637 + (player.x - player.field_1637) * delta),
          -(player.field_1638 + (player.y - player.field_1638) * delta),
          -(player.field_1639 + (player.z - player.field_1639) * delta));
      GL11.glDisable(GL11.GL_TEXTURE_2D);
      GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_FOG);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glDepthMask(false);
      if (s.throughWalls) GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glLineWidth(s.lineWidth);
      int c = COLORS[s.color];
      GL11.glColor4f(
          (c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f, s.opacity / 100f);
      if (e.pos1 != null && e.pos2 != null) box(e.region());
      if (e.pos1 != null) {
        GL11.glColor4f(1, .3f, .3f, s.opacity / 100f);
        box(Region.between(e.pos1, e.pos1));
      }
      if (e.pos2 != null) {
        GL11.glColor4f(.3f, 1, .5f, s.opacity / 100f);
        box(Region.between(e.pos2, e.pos2));
      }
    } finally {
      GL11.glPopMatrix();
      GL11.glPopAttrib();
    }
  }

  private static void box(Region r) {
    double[] lo = {r.min().x() - .003, r.min().y() - .003, r.min().z() - .003},
        hi = {r.max().x() + 1.003, r.max().y() + 1.003, r.max().z() + 1.003};
    GL11.glBegin(GL11.GL_LINES);
    for (int axis = 0; axis < 3; axis++)
      for (int a = 0; a < 2; a++)
        for (int b = 0; b < 2; b++)
          for (int end = 0; end < 2; end++) {
            double[] v = {lo[0], lo[1], lo[2]};
            v[axis] = end == 0 ? lo[axis] : hi[axis];
            v[(axis + 1) % 3] = a == 0 ? lo[(axis + 1) % 3] : hi[(axis + 1) % 3];
            v[(axis + 2) % 3] = b == 0 ? lo[(axis + 2) % 3] : hi[(axis + 2) % 3];
            GL11.glVertex3d(v[0], v[1], v[2]);
          }
    GL11.glEnd();
  }
}
