package local.luke.power.flexible;

import local.luke.power.flexible.mixin.MinecraftAccessor;
import net.minecraft.class_212;
import net.minecraft.class_27;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.opengl.GL11;

/** All GL changes are restored, including exceptional exits. */
public final class PlacementOverlay {
  private static final int[] COLORS = {0x487BFF, 0x33DDEE, 0xFFA040, 0x55DD77};
  private static final double[][][] REGIONS = {
    {{.25, .25}, {.75, .25}, {.75, .75}, {.25, .75}},
    {{0, 0}, {.25, .25}, {.25, .75}, {0, 1}},
    {{1, 0}, {.75, .25}, {.75, .75}, {1, 1}},
    {{0, 0}, {.25, .25}, {.75, .25}, {1, 0}},
    {{0, 1}, {.25, .75}, {.75, .75}, {1, 1}}
  };

  private PlacementOverlay() {}

  public static void render(PlayerEntity player, class_27 hit, float delta) {
    Minecraft mc = MinecraftAccessor.instance();
    if (!FlexiblePlacement.canOperate(mc)
        || player != mc.player
        || !FlexiblePlacement.modes().active()
        || hit == null
        || hit.field_1983 != class_212.TILE) return;
    var settings = FlexiblePlacement.settings();
    if (!settings.showOverlay && !settings.showPreview) return;
    PlacementPlan plan =
        FlexiblePlacement.plan(
            mc,
            player.inventory.getSelectedItem(),
            hit.field_1984,
            hit.field_1985,
            hit.field_1986,
            hit.field_1987);
    if (plan == null) return;
    Direction face = Direction.values()[hit.field_1987], forward = Direction.horizontal(player.yaw);
    double plane =
        face.x != 0
            ? hit.field_1988.x - hit.field_1984
            : face.y != 0 ? hit.field_1988.y - hit.field_1985 : hit.field_1988.z - hit.field_1986;
    plane += .004 * (face.x + face.y + face.z);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glPushMatrix();
    try {
      GL11.glTranslated(
          -(player.field_1637 + (player.x - player.field_1637) * delta),
          -(player.field_1638 + (player.y - player.field_1638) * delta),
          -(player.field_1639 + (player.z - player.field_1639) * delta));
      GL11.glDisable(GL11.GL_TEXTURE_2D);
      GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_CULL_FACE);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glDepthMask(false);
      GL11.glLineWidth(2);
      if (settings.showOverlay && FlexiblePlacement.modes().grid()) {
        for (int i = 0; i < REGIONS.length; i++) {
          color(
              i == plan.part().ordinal() ? COLORS[settings.overlayColor] : 0xEEEEEE,
              i == plan.part().ordinal() ? settings.overlayOpacity / 100f : .12f);
          GL11.glBegin(GL11.GL_QUADS);
          for (double[] uv : REGIONS[i]) vertex(face, forward, uv[0], uv[1], plane, hit);
          GL11.glEnd();
          color(0xFFFFFF, .8f);
          GL11.glBegin(GL11.GL_LINE_LOOP);
          for (double[] uv : REGIONS[i]) vertex(face, forward, uv[0], uv[1], plane, hit);
          GL11.glEnd();
        }
      }
      if (settings.showPreview) {
        color(plan.valid() ? 0x66FF88 : 0xFF5555, .85f);
        box(plan.destination());
        if (plan.facing() != null)
          arrow(plan.destination(), plan.reverse() ? plan.facing().opposite() : plan.facing());
      }
    } finally {
      GL11.glPopMatrix();
      GL11.glPopAttrib();
    }
  }

  private static void color(int c, float alpha) {
    GL11.glColor4f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f, alpha);
  }

  private static void vertex(
      Direction face, Direction forward, double u, double v, double plane, class_27 h) {
    double[] p = FaceGrid.point(face, forward, u, v, plane);
    GL11.glVertex3d(h.field_1984 + p[0], h.field_1985 + p[1], h.field_1986 + p[2]);
  }

  private static void box(BlockPos p) {
    GL11.glBegin(GL11.GL_LINES);
    for (int axis = 0; axis < 3; axis++)
      for (int a = 0; a < 2; a++)
        for (int b = 0; b < 2; b++) {
          double[] v = {p.x() - .002, p.y() - .002, p.z() - .002};
          v[(axis + 1) % 3] += a * 1.004;
          v[(axis + 2) % 3] += b * 1.004;
          GL11.glVertex3d(v[0], v[1], v[2]);
          v[axis] += 1.004;
          GL11.glVertex3d(v[0], v[1], v[2]);
        }
    GL11.glEnd();
  }

  private static void arrow(BlockPos p, Direction d) {
    double x = p.x() + .5, y = p.y() + .5, z = p.z() + .5;
    GL11.glBegin(GL11.GL_LINES);
    GL11.glVertex3d(x, y, z);
    GL11.glVertex3d(x + d.x * .8, y + d.y * .8, z + d.z * .8);
    GL11.glEnd();
  }
}
