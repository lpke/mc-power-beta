package local.luke.power.map;

import static local.luke.power.map.MapMath.*;

import java.nio.ByteBuffer;
import java.util.*;
import local.luke.power.render.RenderControls;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/** One texture cache for both maps. Uploads and all GL access remain on the render thread. */
public final class MapRenderer {
  private record Texture(int id, MapTile owner, long revision, int style) {}

  private static final LinkedHashMap<Key, Texture> TEXTURES = new LinkedHashMap<>(64, .75f, true);
  private static final ByteBuffer BUFFER = BufferUtils.createByteBuffer(PIXELS * 4);
  private static MapCache cache;
  private static int remaining;
  private static float delta = 1;
  private static double pixelsPerUnit = 1;
  private static boolean pending;
  private static int skyDarkness;

  private MapRenderer() {}

  public static void beginFrame() {
    beginFrame(1);
  }

  public static void beginFrame(float tickDelta) {
    remaining = MapConfig.current().uploadsPerFrame;
    pending = false;
    delta = tickDelta;
    var mc = (Minecraft) net.fabricmc.loader.api.FabricLoader.getInstance().getGameInstance();
    // Native sky darkness includes weather and the world-rule daylight clock. Integer
    // light steps avoid invalidating the tile/buffer caches on every rendered frame.
    skyDarkness = mc.world == null || RenderControls.fullBright() ? 0 : mc.world.method_151(1);
    var scaled = new net.minecraft.class_564(mc.options, mc.displayWidth, mc.displayHeight);
    pixelsPerUnit = (double) mc.displayWidth / scaled.method_1857();
  }

  public static void uploadBudget(int uploads) { remaining = uploads; }

  public static int style(MapSettings settings) {
    boolean fullBright = RenderControls.fullBright();
    return Objects.hash(settings.style(fullBright), fullBright ? 0 : skyDarkness);
  }

  /** Returns zero until an immutable disk result and an upload slot are available. */
  static int tile(MapCache source, Key key, MapSettings settings, boolean local, long seed) {
    if (source != cache) { clear(); cache = source; }
    key = source.viewKey(key);
    MapTile tile = source.visible(key);
    if (tile == null) return 0;
    int style = Objects.hash(style(settings), settings.chunkGrid, settings.slimeChunks,
        seed, local, settings.mapSmoothing);
    Texture texture = TEXTURES.get(key);
    if (texture == null || texture.owner != tile || texture.revision != tile.revision || texture.style != style) {
      if (remaining <= 0) return 0;
      remaining--;
      texture = upload(tile, texture, settings, style, local, seed);
      TEXTURES.put(key, texture);
      while (TEXTURES.size() > settings.cacheTiles) {
        var it = TEXTURES.values().iterator();
        GL11.glDeleteTextures(it.next().id);
        it.remove();
      }
    }
    return texture.id;
  }

  public static boolean pending() {
    return pending;
  }

  public static double pixelsPerUnit() {
    return pixelsPerUnit;
  }

  public static void clear() {
    for (Texture t : TEXTURES.values()) GL11.glDeleteTextures(t.id);
    TEXTURES.clear();
    cache = null;
  }

  public static void terrain(
      MapCache source,
      MapProjection view,
      int dimension,
      int layer,
      boolean circle,
      MapSettings settings,
      boolean local,
      long seed) {
    terrain(source, view, dimension, layer, circle, settings, local, seed, pixelsPerUnit);
  }

  public static void terrain(
      MapCache source,
      MapProjection view,
      int dimension,
      int layer,
      boolean circle,
      MapSettings settings,
      boolean local,
      long seed,
      double pixelScale) {
    terrain(source, view, dimension, layer, circle, settings, local, seed, pixelScale, true);
  }

  /** Secondary previews share admission without replacing or canceling the main view's reads. */
  public static void terrain(MapCache source, MapProjection view, int dimension, int layer,
      boolean circle, MapSettings settings, boolean local, long seed, double pixelScale, boolean ownsInterest) {
    if (source != cache) {
      clear();
      cache = source;
    }
    var coverage = MapCoverage.select(view, circle, settings.cacheTiles, pixelScale);
    if (ownsInterest) source.visibleView(dimension, layer, coverage);
    source.publish(4, System.nanoTime() + 250_000);
    int level = coverage.level(), span = SIZE << level;
    int minX = coverage.minX(),
        maxX = coverage.maxX(),
        minZ = coverage.minZ(),
        maxZ = coverage.maxZ();
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    try {
      GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glDisable(GL11.GL_CULL_FACE);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glAlphaFunc(GL11.GL_GREATER, 0);
      GL11.glDisable(GL11.GL_TEXTURE_2D);
      GL11.glColor4d(0, 0, 0, settings.opacity);
      polygon(
          view.clip(
              List.of(
                  new MapProjection.Point(view.left, view.top, 0, 0),
                  new MapProjection.Point(view.left + view.width, view.top, 0, 0),
                  new MapProjection.Point(view.left + view.width, view.top + view.height, 0, 0),
                  new MapProjection.Point(view.left, view.top + view.height, 0, 0)),
              circle));
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      GL11.glColor4d(1, 1, 1, settings.opacity);
      int style =
          Objects.hash(
              style(settings),
              settings.chunkGrid,
              settings.slimeChunks,
              seed,
              local, settings.mapSmoothing);
      int centerX = Math.max(minX, Math.min(maxX, (int)Math.floor(view.x / span)));
      int centerZ = Math.max(minZ, Math.min(maxZ, (int)Math.floor(view.z / span)));
      for (var cell : MapTileSweep.nearest(coverage, centerX, centerZ)) {
          int x = cell.x(), z = cell.z();
          Key key = source.viewKey(new Key(dimension, layer, level, x, z));
          double wx = key.minX(), wz = key.minZ();
          var a = view.screen(wx, wz);
          var b = view.screen(wx + span, wz);
          var c = view.screen(wx + span, wz + span);
          var d = view.screen(wx, wz + span);
          var polygon =
              view.clip(
                  List.of(
                      new MapProjection.Point(a.x(), a.y(), 0, 0),
                      new MapProjection.Point(b.x(), b.y(), 1, 0),
                      new MapProjection.Point(c.x(), c.y(), 1, 1),
                      new MapProjection.Point(d.x(), d.y(), 0, 1)),
                  circle);
          if (polygon.isEmpty()) continue;
          MapTile tile = ownsInterest ? source.visible(key) : source.secondary(key);
          if (tile == null) {
            pending = true;
            continue;
          }
          Texture texture = TEXTURES.get(key);
          if ((texture == null
                  || texture.owner != tile
                  || texture.revision != tile.revision
                  || texture.style != style)
              && remaining > 0) {
            remaining--;
            texture = upload(tile, texture, settings, style, local, seed);
            TEXTURES.put(key, texture);
          }
          if (texture == null || texture.revision != tile.revision || texture.style != style)
            pending = true;
          if (texture == null) continue;
          GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.id);
          GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER,
              settings.mapSmoothing ? GL11.GL_LINEAR : GL11.GL_NEAREST);
          polygon(polygon);
        }
      while (TEXTURES.size() > settings.cacheTiles) {
        var it = TEXTURES.entrySet().iterator();
        var entry = it.next();
        GL11.glDeleteTextures(entry.getValue().id);
        it.remove();
      }
    } finally {
      GL11.glPopAttrib();
    }
  }

  private static Texture upload(
      MapTile tile, Texture previous, MapSettings s, int style, boolean local, long seed) {
    int id = previous == null ? GL11.glGenTextures() : previous.id;
    boolean fullBright = RenderControls.fullBright();
    BUFFER.clear();
    for (int i = 0; i < PIXELS; i++) {
      int color = tile.color(i, s, fullBright, skyDarkness);
      if (color != 0) {
        int x = (int) tile.key.minX() + ((i % SIZE) << tile.key.level()),
            z = (int) tile.key.minZ() + ((i / SIZE) << tile.key.level());
        if (s.slimeChunks && local && tile.key.dimension() == 0 && slime(seed, x >> 4, z >> 4))
          color = 0xff000000 | mix(color, 0x44e855, .28);
        if (s.chunkGrid && tile.key.level() <= 2 && ((x & 15) == 0 || (z & 15) == 0))
          color = 0xff000000 | shade(color, .55);
      }
      BUFFER
          .put((byte) (color >> 16))
          .put((byte) (color >> 8))
          .put((byte) color)
          .put((byte) (color >>> 24));
    }
    BUFFER.flip();
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER,
        s.mapSmoothing ? GL11.GL_LINEAR : GL11.GL_NEAREST);
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    GL11.glTexParameteri(
        GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE);
    GL11.glTexParameteri(
        GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE);
    GL11.glPushClientAttrib(GL11.GL_CLIENT_PIXEL_STORE_BIT);
    try {
      GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
      GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
      GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
      GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
      GL11.glTexImage2D(
          GL11.GL_TEXTURE_2D,
          0,
          GL11.GL_RGBA,
          SIZE,
          SIZE,
          0,
          GL11.GL_RGBA,
          GL11.GL_UNSIGNED_BYTE,
          BUFFER);
    } finally {
      GL11.glPopClientAttrib();
    }
    return new Texture(id, tile, tile.revision, style);
  }

  static void polygon(List<MapProjection.Point> points) {
    if (points.size() < 3) return;
    GL11.glBegin(GL11.GL_TRIANGLE_FAN);
    for (var p : points) {
      GL11.glTexCoord2d(p.u(), p.v());
      GL11.glVertex2d(p.x(), p.y());
    }
    GL11.glEnd();
  }

  public static void dot(double x, double y, int rgb, double size) {
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glColor4ub((byte) (rgb >> 16), (byte) (rgb >> 8), (byte) rgb, (byte) 255);
    GL11.glBegin(GL11.GL_QUADS);
    GL11.glVertex2d(x - size, y - size);
    GL11.glVertex2d(x + size, y - size);
    GL11.glVertex2d(x + size, y + size);
    GL11.glVertex2d(x - size, y + size);
    GL11.glEnd();
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glColor4f(1, 1, 1, 1);
  }

  private static void diamond(double x, double y, int color, double size) {
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    for (int outline = 1; outline >= 0; outline--) {
      double r = size + outline * .8;
      int rgb = outline == 1 ? 0 : color;
      GL11.glColor4ub((byte) (rgb >> 16), (byte) (rgb >> 8), (byte) rgb, (byte) 255);
      GL11.glBegin(GL11.GL_QUADS);
      GL11.glVertex2d(x, y - r);
      GL11.glVertex2d(x + r, y);
      GL11.glVertex2d(x, y + r);
      GL11.glVertex2d(x - r, y);
      GL11.glEnd();
    }
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glColor4f(1, 1, 1, 1);
  }

  public static void arrow(double x, double y, double angle, int color) {
    arrow(x, y, angle, color, 1, 1);
  }

  private static void arrow(double x, double y, double angle, int color, double size, double opacity) {
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glPushMatrix();
    try {
      GL11.glTranslated(x, y, 0);
      GL11.glRotated(Math.toDegrees(angle), 0, 0, 1);
      GL11.glScaled(size, size, 1);
      GL11.glDisable(GL11.GL_TEXTURE_2D);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      for (int outline = 1; outline >= 0; outline--) {
        GL11.glPushMatrix();
        double scale = outline == 1 ? 1.3 : 1;
        GL11.glScaled(scale, scale, 1);
        int rgb = outline == 1 ? 0 : color;
        color(rgb, opacity);
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex2d(0, -5);
        GL11.glVertex2d(3.5, 4);
        GL11.glVertex2d(0, 2);
        GL11.glVertex2d(0, -5);
        GL11.glVertex2d(0, 2);
        GL11.glVertex2d(-3.5, 4);
        GL11.glEnd();
        GL11.glPopMatrix();
      }
    } finally {
      GL11.glPopMatrix();
      GL11.glPopAttrib();
    }
  }

  /** Two shaded faces give the default arrow its raised center seam at every UI scale. */
  public static void player(double x, double y, double angle, MapSettings settings) {
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glPushMatrix();
    try {
      GL11.glTranslated(x, y, 0);
      GL11.glRotated(Math.toDegrees(angle), 0, 0, 1);
      GL11.glScaled(settings.playerSize / 100d, settings.playerSize / 100d, 1);
      GL11.glDisable(GL11.GL_TEXTURE_2D);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      for (int outline = 1; outline >= 0; outline--) {
        GL11.glPushMatrix();
        double size = outline == 1 ? 1.25 : 1;
        GL11.glScaled(size, size, 1);
        int rgb = outline == 1 ? 0x191919 : settings.playerColor;
        if (settings.playerIcon == 3) {
          color(rgb, settings.playerOpacity);
          GL11.glBegin(GL11.GL_TRIANGLE_FAN);
          GL11.glVertex2d(0, 0);
          for (int i = 0; i <= 16; i++) {
            double a = i * Math.PI / 8;
            GL11.glVertex2d(Math.cos(a) * 3.5, Math.sin(a) * 3.5);
          }
          GL11.glEnd();
        } else {
          double base = settings.playerIcon == 2 ? 4 : 2;
          GL11.glBegin(GL11.GL_TRIANGLES);
          color(outline == 0 && settings.playerIcon == 0 ? shade(rgb, 1.25) : rgb, settings.playerOpacity);
          GL11.glVertex2d(0, -5);
          GL11.glVertex2d(0, base);
          GL11.glVertex2d(-3.5, 4);
          color(outline == 0 && settings.playerIcon == 0 ? shade(rgb, .65) : rgb, settings.playerOpacity);
          GL11.glVertex2d(0, -5);
          GL11.glVertex2d(3.5, 4);
          GL11.glVertex2d(0, base);
          GL11.glEnd();
        }
        GL11.glPopMatrix();
      }
    } finally {
      GL11.glPopMatrix();
      GL11.glPopAttrib();
    }
  }

  private static void color(int rgb, double alpha) {
    GL11.glColor4d((rgb >> 16 & 255) / 255d, (rgb >> 8 & 255) / 255d, (rgb & 255) / 255d, alpha);
  }

  private static void label(
      Minecraft mc, MapProjection view, boolean circle, String text, int x, int y, int color) {
    label(mc, view, circle, text, x, y, 1, color);
  }

  private static void label(Minecraft mc, MapProjection view, boolean circle, String text,
      double x, double y, double scale, int color) {
    double width = mc.textRenderer.getWidth(text) * scale, height = 8 * scale;
    if (view.contains(x, y, circle)
        && view.contains(x + width, y, circle)
        && view.contains(x, y + height, circle)
        && view.contains(x + width, y + height, circle)) {
      GL11.glPushMatrix();
      try {
        GL11.glTranslated(x, y, 0); GL11.glScaled(scale, scale, 1);
        mc.textRenderer.drawWithShadow(text, 0, 0, color);
      } finally { GL11.glPopMatrix(); }
    }
  }

  public static void markers(
      Minecraft mc, MapProjection view, int dimension, boolean circle, boolean mini) {
    markers(mc, view, dimension, circle, mini, MapConfig.current());
  }

  public static void markers(
      Minecraft mc, MapProjection view, int dimension, boolean circle, boolean mini, MapSettings s) {
    var pose = Maps.framePose(mc, delta);
    MapLightShading.draw(mc, view, dimension, circle, mini, s);
    var store = Maps.waypoints();
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    try {
      GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_CULL_FACE);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      if (s.footsteps) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4d((s.pathColor >> 16 & 255) / 255d,
            (s.pathColor >> 8 & 255) / 255d, (s.pathColor & 255) / 255d, s.pathOpacity);
        GL11.glBegin(GL11.GL_LINES);
        Maps.Step previous = null;
        for (var step : Maps.path()) {
          if (previous != null
              && previous.dimension() == dimension
              && step.dimension() == dimension) {
            var a = view.screen(previous.x(), previous.z());
            var b = view.screen(step.x(), step.z());
            if (view.contains(a.x(), a.y(), circle) && view.contains(b.x(), b.y(), circle)) {
              GL11.glVertex2d(a.x(), a.y());
              GL11.glVertex2d(b.x(), b.y());
            }
          }
          previous = step;
        }
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1, 1, 1, 1);
      }
      int waypointCount = 0;
      if (s.waypoints && store != null)
        for (var point : Maps.points()) {
          if (waypointCount >= 512) break;
          if (point.death && !s.deathpoints) continue;
          if (MinimapArrows.destination(point)) {
            if (!point.enabled || point.dimension != dimension) continue;
          } else if (!point.visible(
              dimension, store.selectedSet(), store.allSets(), Maps.x(), Maps.z(), s)) continue;
          var p = view.screen(point.x + .5, point.z + .5);
          waypointCount++;
          double markerScale = mini ? 1 : s.worldMapWaypointSize / 100d;
          if (!view.contains(p.x(), p.y(), circle, Math.max(6, 6 * markerScale))) {
            var style = MinimapArrows.style(point, s, mini);
            if (!style.visible()) continue;
            var edge = view.edge(p, circle, style.inset());
            arrow(
                edge.x(),
                edge.y(),
                Math.atan2(p.y() - view.top - view.height / 2, p.x() - view.left - view.width / 2)
                    + Math.PI / 2,
                style.color(), style.scale(), style.opacity());
            continue;
          }
          int waypointColor = WaypointSymbols.color(point, s);
          double waypointOpacity = WaypointSymbols.opacity(point, s);
          if (waypointOpacity <= 0) continue;
          double radius = mini ? point.death ? 4 : 2.5 : 4.5 * markerScale;
          WaypointSymbols.draw(mc, point, p.x(), p.y(), radius,
              waypointColor, waypointOpacity);
          if (point.death || s.waypointNames) {
            String text = point.death ? mini ? WaypointTimes.time(point.created) : WaypointTimes.compact(point.created)
                : mini ? point.abbreviation() : MinimapArrows.destination(point)
                ? point.x + "," + point.y + "," + point.z : point.name;
            label(mc, view, circle, text, p.x() + (mini ? radius + 1.5 : 9 * markerScale), p.y() - 4 * markerScale,
                markerScale, (int)Math.round(waypointOpacity * 255) << 24 | (point.death ? 0xff5555 : waypointColor));
          }
        }
      if (s.radar && dimension == Maps.dimension())
        for (var entity : Maps.radar()) {
          var p =
              view.screen(
                  MapMotion.position(
                      entity.previousX(), entity.x(), Maps.radarDelta(entity, delta)),
                  MapMotion.position(
                      entity.previousZ(), entity.z(), Maps.radarDelta(entity, delta)));
          if (!view.contains(p.x(), p.y(), circle, 2)) continue;
          double dy = MapMotion.position(entity.previousY(), entity.y(), Maps.radarDelta(entity, delta)) - pose.y();
          int color = MapLighting.radarColor(entity.color(), dy, s);
          diamond(p.x(), p.y(), color, entity.item() ? .7 : 1.3);
          if (s.entityHeights && Math.abs(dy) > 3) {
            double cy = p.y() + (dy > 0 ? -3.5 : 3.5), tip = cy + (dy > 0 ? -1 : 1);
            if (view.contains(p.x(), tip, circle, 2)) {
              GL11.glDisable(GL11.GL_TEXTURE_2D);
              GL11.glColor4ub((byte)(color >> 16), (byte)(color >> 8), (byte)color, (byte)180);
              GL11.glBegin(GL11.GL_LINES);
              GL11.glVertex2d(p.x() - 1.5, cy); GL11.glVertex2d(p.x(), tip);
              GL11.glVertex2d(p.x(), tip); GL11.glVertex2d(p.x() + 1.5, cy);
              GL11.glEnd(); GL11.glEnable(GL11.GL_TEXTURE_2D);
            }
          }
          if (s.entityNames)
            label(
                mc, view, circle, entity.name(), (int) p.x() + 3, (int) p.y() + 3, color);
        }
      if (dimension == Maps.dimension()) {
        var p = view.screen(pose.x(), pose.z());
        if (view.contains(p.x(), p.y(), circle, 6)) {
          GL11.glPushMatrix();
          GL11.glTranslated(p.x(), p.y(), 0);
          if (mini) GL11.glScaled(.55, .55, 1);
          player(0, 0, Math.toRadians(pose.yaw() + 180) + view.angle, s);
          GL11.glPopMatrix();
        }
      }
    } finally {
      GL11.glPopAttrib();
    }
  }
}
