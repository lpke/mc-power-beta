package local.luke.power.map;

import static local.luke.power.map.MapMath.*;

import java.nio.ByteBuffer;
import java.util.*;
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

  private MapRenderer() {}

  public static void beginFrame() {
    remaining = MapConfig.current().uploadsPerFrame;
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
    if (source != cache) {
      clear();
      cache = source;
    }
    var coverage = MapCoverage.select(view, circle, settings.cacheTiles);
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
      GL11.glColor4d(.035, .035, .045, settings.opacity);
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
              settings.style(),
              settings.chunkGrid,
              settings.slimeChunks,
              settings.lightOverlay,
              seed,
              local);
      for (int z = minZ; z <= maxZ; z++)
        for (int x = minX; x <= maxX; x++) {
          Key key = new Key(dimension, layer, level, x, z);
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
          MapTile tile = source.get(key);
          if (tile == null) continue;
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
          if (texture == null) continue;
          GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.id);
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
    BUFFER.clear();
    for (int i = 0; i < PIXELS; i++) {
      int colour = tile.colour(i, s);
      if (colour != 0) {
        int x = (int) tile.key.minX() + ((i % SIZE) << tile.key.level()),
            z = (int) tile.key.minZ() + ((i / SIZE) << tile.key.level());
        if (s.slimeChunks && local && tile.key.dimension() == 0 && slime(seed, x >> 4, z >> 4))
          colour = 0xff000000 | mix(colour, 0x44e855, .28);
        if (s.lightOverlay && tile.light(i) < 8) colour = 0xff000000 | mix(colour, 0xf44736, .35);
        if (s.chunkGrid && tile.key.level() <= 2 && ((x & 15) == 0 || (z & 15) == 0))
          colour = 0xff000000 | shade(colour, .55);
      }
      BUFFER
          .put((byte) (colour >> 16))
          .put((byte) (colour >> 8))
          .put((byte) colour)
          .put((byte) (colour >>> 24));
    }
    BUFFER.flip();
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
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

  private static void polygon(List<MapProjection.Point> points) {
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

  public static void arrow(double x, double y, double angle, int colour) {
    GL11.glPushMatrix();
    GL11.glTranslated(x, y, 0);
    GL11.glRotated(Math.toDegrees(angle), 0, 0, 1);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glColor4ub((byte) (colour >> 16), (byte) (colour >> 8), (byte) colour, (byte) 255);
    GL11.glBegin(GL11.GL_TRIANGLES);
    GL11.glVertex2d(0, -5);
    GL11.glVertex2d(3.5, 4);
    GL11.glVertex2d(0, 2);
    GL11.glVertex2d(0, -5);
    GL11.glVertex2d(0, 2);
    GL11.glVertex2d(-3.5, 4);
    GL11.glEnd();
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glColor4f(1, 1, 1, 1);
    GL11.glPopMatrix();
  }

  private static void label(
      Minecraft mc, MapProjection view, boolean circle, String text, int x, int y, int colour) {
    int width = mc.textRenderer.getWidth(text);
    if (view.contains(x, y, circle)
        && view.contains(x + width, y, circle)
        && view.contains(x, y + 8, circle)
        && view.contains(x + width, y + 8, circle))
      mc.textRenderer.drawWithShadow(text, x, y, colour);
  }

  public static void markers(
      Minecraft mc, MapProjection view, int dimension, boolean circle, boolean mini) {
    MapSettings s = MapConfig.current();
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
        GL11.glColor4f(1, 1, .6f, .7f);
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
          if (!point.visible(
              dimension, store.selectedSet(), store.allSets(), Maps.x(), Maps.z(), s)) continue;
          var p = view.screen(point.x + .5, point.z + .5);
          waypointCount++;
          if (!view.contains(p.x(), p.y(), circle, 6)) {
            if (!mini) continue;
            var edge = view.edge(p, circle, 7);
            arrow(
                edge.x(),
                edge.y(),
                Math.atan2(p.y() - view.top - view.height / 2, p.x() - view.left - view.width / 2)
                    + Math.PI / 2,
                point.colour);
            continue;
          }
          dot(p.x(), p.y(), point.colour, 2.5);
          if (s.waypointNames && !mini)
            mc.textRenderer.drawWithShadow(
                point.name, (int) p.x() + 5, (int) p.y() - 4, point.colour);
        }
      if (s.radar && dimension == Maps.dimension())
        for (var entity : Maps.radar()) {
          var p = view.screen(entity.x(), entity.z());
          if (!view.contains(p.x(), p.y(), circle, 2)) continue;
          dot(p.x(), p.y(), entity.colour(), entity.item() ? 1 : 1.6);
          if (s.entityHeights && Math.abs(entity.y() - Maps.y()) > 3)
            label(
                mc,
                view,
                circle,
                entity.y() > Maps.y() ? "+" : "-",
                (int) p.x() + 2,
                (int) p.y() - 4,
                entity.colour());
          if (s.entityNames)
            label(
                mc, view, circle, entity.name(), (int) p.x() + 3, (int) p.y() + 3, entity.colour());
        }
      if (dimension == Maps.dimension()) {
        var p = view.screen(Maps.x(), Maps.z());
        if (view.contains(p.x(), p.y(), circle, 6))
          arrow(p.x(), p.y(), Math.toRadians(Maps.yaw() + 180) + view.angle, 0xffffff);
      }
    } finally {
      GL11.glPopAttrib();
    }
  }
}
