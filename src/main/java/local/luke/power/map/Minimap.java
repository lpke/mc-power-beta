package local.luke.power.map;

import java.util.ArrayList;
import java.util.List;
import local.luke.power.input.Bindings;
import net.minecraft.class_564;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public final class Minimap {
  public record Layout(int x, int y, int size) {}

  public record Box(int x, int y, int width, int height) {}

  public record Label(String text, int x, int y) {}

  public record Frame(Layout layout, Box bounds, List<Label> labels) {}

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
    Layout layout = layout(settings, width, height, enlarged);
    var labels = new ArrayList<Label>();
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
    int left = layout.x - 2, right = layout.x + layout.size + 2, y = layout.y + layout.size + 3;
    for (String text : lines) {
      int textWidth = mc.textRenderer.getWidth(text),
          x =
              Math.max(
                  2, Math.min(width - textWidth - 2, layout.x + (layout.size - textWidth) / 2));
      labels.add(new Label(text, x, y));
      left = Math.min(left, x);
      right = Math.max(right, x + textWidth);
      y += 10;
    }
    return new Frame(
        layout,
        new Box(
            left,
            layout.y - 2,
            right - left,
            layout.size + (lines.isEmpty() ? 4 : 5 + lines.size() * 10)),
        List.copyOf(labels));
  }

  private Minimap() {}

  public static Layout layout(MapSettings settings, int width, int height, boolean enlarged) {
    int info =
        (settings.coordinates ? 10 : 0) + (settings.biome ? 10 : 0) + (settings.clock ? 10 : 0);
    if (info > 0) info += 3;
    int size =
        Math.max(
            24,
            Math.min(
                enlarged ? settings.size * 2 : settings.size,
                Math.min(width - 12, height - 12 - info)));
    int x = (settings.corner % 2 == 0 ? settings.offsetX : width - size - settings.offsetX);
    int y = (settings.corner < 2 ? settings.offsetY : height - size - info - settings.offsetY);
    if (enlarged) {
      x = (width - size) / 2;
      y = (height - size - info) / 2;
    }
    return new Layout(
        Math.max(2, Math.min(width - size - 2, x)),
        Math.max(2, Math.min(height - size - info - 2, y)),
        size);
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
        || mc.currentScreen != null) return;
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
    return frame.layout;
  }

  public static void drawFrame(Minecraft mc, MapSettings s, Frame frame, boolean preview) {
    drawFrame(mc, s, frame, preview, 1);
  }

  public static void drawFrame(
      Minecraft mc, MapSettings s, Frame frame, boolean preview, float delta) {
    if (preview) { MapRenderer.beginFrame(delta); Maps.previewRadar(s); }
    Layout bounds = frame.layout;
    var pose = Maps.framePose(mc, delta);
    double angle = s.rotate ? Math.toRadians(-pose.yaw() - 180) : 0;
    var view =
        new MapProjection(
            pose.x(), pose.z(), s.zoom, bounds.x, bounds.y, bounds.size, bounds.size, angle);
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
              mc.world.getSeed());
        MapRenderer.markers(mc, view, Maps.dimension(), s.circular, true, s);
      } else {
        // A fixed sample never starts world sampling, disk I/O or another world in the HUD editor.
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBegin(GL11.GL_QUADS);
        for (int z = 0; z < bounds.size; z += 4)
          for (int x = 0; x < bounds.size; x += 4) {
            if (!view.contains(bounds.x + x + 2, bounds.y + z + 2, s.circular)) continue;
            double river = Math.sin(z * .08) * 12 + bounds.size * .55;
            int color =
                Math.abs(x - river) < 7
                    ? 0x4167ab
                    : MapMath.shade(
                        x > bounds.size * .75 ? 0x77756c : 0x6b913e,
                        .85 + Math.sin(x * .09 + z * .06) * .13);
            GL11.glColor4ub(
                (byte) (color >> 16),
                (byte) (color >> 8),
                (byte) color,
                (byte) Math.round(s.opacity * 255));
            double px = bounds.x + x, py = bounds.y + z;
            GL11.glVertex2d(px, py);
            GL11.glVertex2d(px + 4, py);
            GL11.glVertex2d(px + 4, py + 4);
            GL11.glVertex2d(px, py + 4);
          }
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        MapRenderer.player(bounds.x + bounds.size / 2d, bounds.y + bounds.size / 2d, 0, s);
      }
      GL11.glDisable(GL11.GL_TEXTURE_2D);
      for (int ring = 2; ring >= 0; ring--) {
        float light = ring == 2 ? 0 : ring == 1 ? .65f : .22f;
        GL11.glColor4f(light, light, light, 1);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        if (s.circular) {
          for (int i = 0; i < 64; i++) {
            double a = i * Math.PI * 2 / 64;
            GL11.glVertex2d(
                bounds.x + bounds.size / 2d + Math.cos(a) * (bounds.size / 2d + ring),
                bounds.y + bounds.size / 2d + Math.sin(a) * (bounds.size / 2d + ring));
          }
        } else {
          GL11.glVertex2d(bounds.x - ring, bounds.y - ring);
          GL11.glVertex2d(bounds.x + bounds.size + ring, bounds.y - ring);
          GL11.glVertex2d(bounds.x + bounds.size + ring, bounds.y + bounds.size + ring);
          GL11.glVertex2d(bounds.x - ring, bounds.y + bounds.size + ring);
        }
        GL11.glEnd();
      }
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      GL11.glColor4f(1, 1, 1, 1);
      if (s.compass)
        for (int i = 0; i < 4; i++) {
          double a = i * Math.PI / 2 + angle;
          double distance = bounds.size / 2d - 6;
          if (!s.circular) distance /= Math.max(Math.abs(Math.sin(a)), Math.abs(Math.cos(a)));
          int tx = (int) (bounds.x + bounds.size / 2d + Math.sin(a) * distance) - 2;
          int ty = (int) (bounds.y + bounds.size / 2d - Math.cos(a) * distance) - 4;
          mc.textRenderer.drawWithShadow(new String[] {"N", "E", "S", "W"}[i], tx, ty, 0xffffff);
        }
      for (Label label : frame.labels)
        mc.textRenderer.drawWithShadow(label.text, label.x, label.y, 0xffffff);
    } finally {
      GL11.glPopAttrib();
    }
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
