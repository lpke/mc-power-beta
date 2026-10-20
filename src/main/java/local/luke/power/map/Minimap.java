package local.luke.power.map;

import java.util.ArrayList;
import local.luke.power.map.MinimapGeometry.*;
import local.luke.power.input.Bindings;
import net.minecraft.class_564;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public final class Minimap {
  public static boolean visible(MapSettings settings, boolean debug) {
    return settings.minimap && (!debug || !settings.hideWithDebug);
  }

  public static Frame frame(
      Minecraft mc,
      MapSettings settings,
      int width,
      int height,
      boolean enlarged,
      boolean preview) {
    var lines = new ArrayList<String>();
    if (settings.coordinates)
      lines.add(
          preview
              ? "128, 64, -256"
              : (int) Math.floor(Maps.x())
                  + ", "
                  + (int) Math.floor(Maps.y())
                  + ", "
                  + (int) Math.floor(Maps.z()));
    if (settings.biome)
      lines.add(
          preview
              ? "Forest"
              : Maps.layer() < 0
                  ? MapPalette.biome(currentBiome())
                  : "Cave · Y " + (settings.caveMode == 2 ? settings.caveTop : (int) Math.floor(Maps.y())));
    if (settings.clock)
      lines.add(preview ? "12:00" : local.luke.power.hud.InfoHud.timeLine().replace("Time: ", ""));
    return MinimapGeometry.frame(settings, width, height, enlarged, lines, mc.textRenderer::getWidth);
  }

  private Minimap() {}

  public static Layout layout(MapSettings settings, int width, int height, boolean enlarged) {
    return MinimapGeometry.layout(settings, width, height, enlarged);
  }

  public static void render(Minecraft mc) {
    render(mc, 1);
  }

  public static void render(Minecraft mc, float delta) {
    MapSettings settings = MapConfig.current();
    if (!visible(settings, mc.options.debugHud)
        || Maps.cache() == null
        || mc.world == null
        || mc.player == null
        || mc.options.hideHud
        || mc.currentScreen != null && !(mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen)) return;
    MapRenderer.beginFrame(delta);
    var screen = new class_564(mc.options, mc.displayWidth, mc.displayHeight);
    var frame =
        frame(
            mc,
            settings,
            screen.method_1857(),
            screen.method_1858(),
            Bindings.down(MapKeys.ENLARGE),
            false);
    drawFrame(mc, settings, frame, false, delta);
  }

  public static Layout draw(
      Minecraft mc, MapSettings s, int width, int height, boolean enlarged, boolean preview) {
    Frame frame = frame(mc, s, width, height, enlarged, preview);
    drawFrame(mc, s, frame, preview);
    return frame.layout();
  }

  public static void drawFrame(Minecraft mc, MapSettings s, Frame frame, boolean preview) {
    drawFrame(mc, s, frame, preview, 1);
  }

  public static void drawFrame(
      Minecraft mc, MapSettings s, Frame frame, boolean preview, float delta) {
    if (preview) { MapRenderer.beginFrame(delta); Maps.previewRadar(s); }
    Layout bounds = frame.layout();
    var pose = Maps.framePose(mc, delta);
    double angle = s.rotate ? Math.toRadians(-pose.yaw() - 180) : 0;
    var view =
        new MapProjection(
            pose.x(), pose.z(), s.zoom, bounds.x(), bounds.y(), bounds.size(), bounds.size(), angle);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    try {
      GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_CULL_FACE);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glAlphaFunc(GL11.GL_GREATER, 0);
      if (Maps.cache() != null && mc.world != null && mc.player != null) {
        if (!MinimapBuffer.draw(mc, view, s))
          MapRenderer.terrain(
              Maps.cache(),
              view,
              Maps.dimension(),
              Maps.layer(),
              s.circular,
              s,
              !mc.world.isRemote,
              mc.world.getSeed(), MapRenderer.pixelsPerUnit(),
              !(mc.currentScreen instanceof local.luke.power.ui.WorldMapScreen));
        MapRenderer.markers(mc, view, Maps.dimension(), s.circular, true, s);
      } else {
        // A fixed sample never starts world sampling, disk I/O or another world in the HUD editor.
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBegin(GL11.GL_QUADS);
        for (int z = 0; z < bounds.size(); z += 4)
          for (int x = 0; x < bounds.size(); x += 4) {
            if (!view.contains(bounds.x() + x + 2, bounds.y() + z + 2, s.circular)) continue;
            double river = Math.sin(z * .08) * 12 + bounds.size() * .55;
            int color =
                Math.abs(x - river) < 7
                    ? 0x4167ab
                    : MapMath.shade(
                        x > bounds.size() * .75 ? 0x77756c : 0x6b913e,
                        .85 + Math.sin(x * .09 + z * .06) * .13);
            GL11.glColor4ub(
                (byte) (color >> 16),
                (byte) (color >> 8),
                (byte) color,
                (byte) Math.round(s.opacity * 255));
            double px = bounds.x() + x, py = bounds.y() + z;
            GL11.glVertex2d(px, py);
            GL11.glVertex2d(px + 4, py);
            GL11.glVertex2d(px + 4, py + 4);
            GL11.glVertex2d(px, py + 4);
          }
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        MapRenderer.player(bounds.x() + bounds.size() / 2d, bounds.y() + bounds.size() / 2d, 0, s);
      }
      MinimapBorder.draw(mc, bounds, s.circular);
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      GL11.glColor4f(1, 1, 1, 1);
      if (s.compass) {
        double scale = s.directionTextSize / 100d;
        String[] directions = {"N", "E", "S", "W"};
        for (int i = 0; i < directions.length; i++) {
          var origin = MinimapGeometry.direction(bounds, i, angle, s.circular,
              mc.textRenderer.getWidth(directions[i]), scale);
          text(mc, directions[i], origin.x(), origin.y(), scale);
        }
      }
      for (Label label : frame.labels()) text(mc, label.text(), label.x(), label.y(), label.scale());
    } finally {
      GL11.glPopAttrib();
    }
  }

  private static void text(Minecraft mc, String text, double x, double y, double scale) {
    GL11.glPushMatrix();
    try {
      GL11.glTranslated(x, y, 0); GL11.glScaled(scale, scale, 1);
      mc.textRenderer.drawWithShadow(text, 0, 0, 0xffffff);
    } finally { GL11.glPopMatrix(); }
  }

  private static int currentBiome() {
    var cache = Maps.cache();
    if (cache == null) return 0;
    var tile =
        cache.peek(
            MapMath.key(
                Maps.dimension(),
                Maps.layer(),
                0,
                (int) Math.floor(Maps.x()),
                (int) Math.floor(Maps.z())));
    return tile == null
        ? 0
        : tile.biome(MapMath.index((int) Math.floor(Maps.x()), (int) Math.floor(Maps.z())));
  }
}
