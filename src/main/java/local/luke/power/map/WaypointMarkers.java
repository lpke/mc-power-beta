package local.luke.power.map;

import java.nio.*;
import java.util.*;
import net.minecraft.class_564;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/** Projection uses the current world render matrices, including freecam, FOV and third person. */
public final class WaypointMarkers {
  private record Label(float x, float y, String text, int color) {}

  private static final FloatBuffer MODEL = BufferUtils.createFloatBuffer(16),
      PROJECTION = BufferUtils.createFloatBuffer(16);
  private static final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(16);
  private static final List<Label> LABELS = new ArrayList<>();
  private static Object world;
  private static final WaypointCamera CAMERA = new WaypointCamera();
  private static final FloatBuffer UNBOBBED = BufferUtils.createFloatBuffer(16);
  private static boolean captureBob;
  private static boolean eligible(Minecraft mc) {
    return MapConfig.current().worldWaypoints && mc.world != null && mc.player != null
        && !mc.options.hideHud && mc.currentScreen == null && Maps.waypoints() != null && !Maps.points().isEmpty();
  }
  public static void beginCamera(Minecraft mc) {
    CAMERA.reset();
    captureBob = eligible(mc) && !local.luke.power.input.DetachedCamera.isActive();
  }
  public static void beforeBob() {
    if (captureBob) { GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MODEL); CAMERA.before(MODEL); }
  }
  public static void afterBob() {
    if (captureBob) { GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MODEL); CAMERA.after(MODEL); }
  }

  private WaypointMarkers() {}

  public static void project(Minecraft mc, float delta) {
    LABELS.clear();
    world = mc.world;
    var settings = MapConfig.current();
    var store = Maps.waypoints();
    if (!eligible(mc)) return;
    var camera = mc.field_2807 == null ? mc.player : mc.field_2807;
    double bx = camera.field_1637 + (camera.x - camera.field_1637) * delta,
        by = camera.field_1638 + (camera.y - camera.field_1638) * delta,
        bz = camera.field_1639 + (camera.z - camera.field_1639) * delta;
    GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MODEL);
    if (!local.luke.power.input.DetachedCamera.isActive()) {
      CAMERA.apply(MODEL, UNBOBBED);
      for (int i = 0; i < 16; i++) MODEL.put(i, UNBOBBED.get(i));
    }
    GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, PROJECTION);
    GL11.glGetInteger(GL11.GL_VIEWPORT, VIEWPORT);
    for (Waypoint p : Maps.points()) {
      if (LABELS.size() >= 256) break;
      if (!p.visible(
          Maps.dimension(), store.selectedSet(), store.allSets(), Maps.x(), Maps.z(), settings))
        continue;
      var projected = WaypointProjection.project(p.x + .5 - bx, p.y + 1 - by,
          p.z + .5 - bz, MODEL, PROJECTION);
      if (projected == null) continue;
      String text =
          (settings.waypointNames ? p.name : "◆")
              + (settings.waypointDistances
                  ? " " + Math.round(p.distance(Maps.x(), Maps.z())) + "m"
                  : "");
      LABELS.add(
          new Label(
              (float) ((VIEWPORT.get(0) + projected.x() * VIEWPORT.get(2)) / mc.displayWidth),
              (float) (1 - (VIEWPORT.get(1) + projected.y() * VIEWPORT.get(3)) / mc.displayHeight), text, p.colour));
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
      GL11.glDisable(GL11.GL_CULL_FACE);
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glEnable(GL11.GL_ALPHA_TEST);
      GL11.glAlphaFunc(GL11.GL_GREATER, 0.01f);
      GL11.glColor4f(1, 1, 1, 1);
      for (Label label : LABELS)
        if (label.x > 0 && label.x < 1 && label.y > 0 && label.y < 1)
          {
            GL11.glPushMatrix();
            GL11.glTranslated(label.x * w - mc.textRenderer.getWidth(label.text) / 2d, label.y * h, 0);
            mc.textRenderer.drawWithShadow(label.text, 0, 0, label.color);
            GL11.glPopMatrix();
          }
    } finally {
      GL11.glPopAttrib();
    }
  }
}
