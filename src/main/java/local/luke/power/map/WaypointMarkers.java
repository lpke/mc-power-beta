package local.luke.power.map;

import java.nio.*;
import java.util.*;
import net.minecraft.class_564;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/** Projection uses the current world render matrices, including freecam, FOV and third person. */
public final class WaypointMarkers {
  private record Label(float x, float y, String text, int color, Waypoint point, double distance) {}

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
        && !mc.options.hideHud && (mc.currentScreen == null || mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen) && Maps.waypoints() != null && !Maps.points().isEmpty();
  }
  public static void beginCamera(Minecraft mc) {
    CAMERA.reset();
    captureBob = eligible(mc) && MapConfig.current().waypointBobbing == 0
        && !local.luke.power.input.DetachedCamera.isActive();
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
    if (settings.waypointBobbing == 0 && !local.luke.power.input.DetachedCamera.isActive()) {
      CAMERA.apply(MODEL, UNBOBBED);
      for (int i = 0; i < 16; i++) MODEL.put(i, UNBOBBED.get(i));
    }
    GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, PROJECTION);
    GL11.glGetInteger(GL11.GL_VIEWPORT, VIEWPORT);
    double cx = bx, cy = by, cz = bz;
    if (local.luke.power.input.RenderedView.valid(mc.world, camera)) {
      cx = local.luke.power.input.RenderedView.x();
      cy = local.luke.power.input.RenderedView.y();
      cz = local.luke.power.input.RenderedView.z();
    } else if (local.luke.power.input.DetachedCamera.isActive()) {
      cx = local.luke.power.input.DetachedCamera.x();
      cy = local.luke.power.input.DetachedCamera.y();
      cz = local.luke.power.input.DetachedCamera.z();
    }
    for (Waypoint p : Maps.points()) {
      if (LABELS.size() >= 256) break;
      if (p.death && !settings.deathpoints) continue;
      if (MinimapArrows.destination(p)) {
        if (!p.enabled || p.dimension != Maps.dimension()) continue;
      } else if (!p.visible(Maps.dimension(), store.selectedSet(), store.allSets(), Maps.x(), Maps.z(), settings)) continue;
      var projected = WaypointProjection.project(p.x + .5 - bx, p.y + 1 - by,
          p.z + .5 - bz, MODEL, PROJECTION);
      if (projected == null) continue;
      double dx = p.x + .5 - cx, dy = p.y + 1 - cy, dz = p.z + .5 - cz;
      LABELS.add(new Label(
          (float)((VIEWPORT.get(0) + projected.x() * VIEWPORT.get(2)) / mc.displayWidth),
          (float)(1 - (VIEWPORT.get(1) + projected.y() * VIEWPORT.get(3)) / mc.displayHeight),
          p.name, WaypointSymbols.color(p, settings), p, Math.sqrt(dx * dx + dy * dy + dz * dz)));
    }
  }

  public static void render(Minecraft mc) {
    if (world != mc.world || mc.options.hideHud || mc.currentScreen != null && !(mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen)) return;
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
      Label aimed = null;
      double closest = 81;
      for (Label label : LABELS) {
        if (WaypointSymbols.opacity(label.point, MapConfig.current()) <= 0) continue;
        double dx = (label.x - .5) * w, dy = (label.y - .5) * h;
        double distance = dx * dx + dy * dy;
        if (distance < closest) { closest = distance; aimed = label; }
      }
      var settings = MapConfig.current();
      for (Label label : LABELS) {
        if (label.x <= 0 || label.x >= 1 || label.y <= 0 || label.y >= 1) continue;
        double opacity = WaypointSymbols.opacity(label.point, settings);
        if (opacity <= 0) continue;
        var content = WaypointLabel.of(label.point, settings, label == aimed, label.distance,
            label.point.distance(Maps.x(), Maps.z()));
        double scale = content.scale();
        GL11.glPushMatrix();
        try {
          GL11.glTranslated(label.x * w, label.y * h, 0);
          GL11.glScaled(scale, scale, 1);
          WaypointSymbols.draw(mc, label.point, 0, 0, 4.5, label.color, opacity);
          int color = (int)Math.round(opacity * 255) << 24 | label.color;
          if (!content.name().isEmpty()) mc.textRenderer.drawWithShadow(content.name(), 9, -4, color);
          if (!content.distance().isEmpty()) {
            int distanceWidth = mc.textRenderer.getWidth(content.distance());
            int distanceX = MinimapArrows.destination(label.point)
                ? 9 + (mc.textRenderer.getWidth(content.name()) - distanceWidth) / 2 : -distanceWidth / 2;
            mc.textRenderer.drawWithShadow(content.distance(), distanceX, 7, color);
          }
        } finally { GL11.glPopMatrix(); }
      }
    } finally {
      GL11.glPopAttrib();
    }
  }
}
