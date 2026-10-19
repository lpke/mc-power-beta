package local.luke.power.map;

import java.nio.*;
import java.util.*;
import net.minecraft.class_564;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

/** Projection uses the current world render matrices, including freecam, FOV and third person. */
public final class WaypointMarkers {
  private record Label(float x, float y, String text, int colour) {}

  private static final FloatBuffer MODEL = BufferUtils.createFloatBuffer(16),
      PROJECTION = BufferUtils.createFloatBuffer(16),
      POINT = BufferUtils.createFloatBuffer(3);
  private static final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(16);
  private static final List<Label> LABELS = new ArrayList<>();
  private static Object world;

  private WaypointMarkers() {}

  public static void project(Minecraft mc, float delta) {
    LABELS.clear();
    world = mc.world;
    var settings = MapConfig.current();
    var store = Maps.waypoints();
    if (!settings.waypoints
        || !settings.worldWaypoints
        || store == null
        || mc.world == null
        || mc.player == null
        || mc.options.hideHud) return;
    var camera = mc.field_2807 == null ? mc.player : mc.field_2807;
    double bx = camera.field_1637 + (camera.x - camera.field_1637) * delta,
        by = camera.field_1638 + (camera.y - camera.field_1638) * delta,
        bz = camera.field_1639 + (camera.z - camera.field_1639) * delta;
    GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MODEL);
    GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, PROJECTION);
    GL11.glGetInteger(GL11.GL_VIEWPORT, VIEWPORT);
    for (Waypoint p : Maps.points()) {
      if (LABELS.size() >= 256) break;
      if (!p.visible(
          Maps.dimension(), store.selectedSet(), store.allSets(), Maps.x(), Maps.z(), settings))
        continue;
      if (!GLU.gluProject(
              (float) (p.x + .5 - bx),
              (float) (p.y + 1 - by),
              (float) (p.z + .5 - bz),
              MODEL,
              PROJECTION,
              VIEWPORT,
              POINT)
          || POINT.get(2) < 0
          || POINT.get(2) >= 1) continue;
      String text =
          (settings.waypointNames ? p.name : "◆")
              + (settings.waypointDistances
                  ? " " + Math.round(p.distance(Maps.x(), Maps.z())) + "m"
                  : "");
      LABELS.add(
          new Label(
              POINT.get(0) / mc.displayWidth, 1 - POINT.get(1) / mc.displayHeight, text, p.colour));
    }
  }

  public static void render(Minecraft mc) {
    if (world != mc.world || mc.options.hideHud || mc.currentScreen != null) return;
    var size = new class_564(mc.options, mc.displayWidth, mc.displayHeight);
    int w = size.method_1857(), h = size.method_1858();
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    try {
      GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glDisable(GL11.GL_LIGHTING);
      for (Label label : LABELS)
        if (label.x > 0 && label.x < 1 && label.y > 0 && label.y < 1)
          mc.textRenderer.drawWithShadow(
              label.text,
              (int) (label.x * w) - mc.textRenderer.getWidth(label.text) / 2,
              (int) (label.y * h),
              label.colour);
    } finally {
      GL11.glPopAttrib();
    }
  }
}
