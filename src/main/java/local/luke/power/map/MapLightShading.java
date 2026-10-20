package local.luke.power.map;

import java.util.ArrayList;
import java.util.List;
import local.luke.power.light.LightOverlay;
import local.luke.power.light.LightSettings;
import local.luke.power.ui.WorldMapScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

/** Independent, bounded spawn-light survey. Render paths consume immutable positions only. */
public final class MapLightShading {
  public record Cell(int x, int z, int length) {}
  private static final LightSettings SAMPLE = new LightSettings();
  private static World world;
  private static String context = "";
  private static int layer, radius, cx, cz, cursor, ticks;
  private static final List<Cell> pending = new ArrayList<>();
  private static List<Cell> cells = List.of();
  public static int checksLastTick;
  static { SAMPLE.spawnableOnly = true; }
  private MapLightShading() {}

  public static void clear() {
    world = null; context = ""; cursor = ticks = checksLastTick = 0; pending.clear(); cells = List.of();
  }

  public static void tick(Minecraft mc, MapSettings settings) {
    checksLastTick = 0;
    if (!settings.lightOverlay || mc.world == null || Maps.cache() == null
        || !settings.minimap && !(settings.worldMap && mc.currentScreen instanceof WorldMapScreen)) {
      if (world != null) clear();
      return;
    }
    int selected = mc.currentScreen instanceof WorldMapScreen screen
        ? screen.lightLayer(Maps.dimension()) : Maps.layer();
    if (selected == Integer.MIN_VALUE) { clear(); return; }
    int x = (int)Math.floor(Maps.x()), z = (int)Math.floor(Maps.z());
    String nextContext = selected < 0 ? "" : Maps.cache().viewContext();
    if (world != mc.world || layer != selected || radius != settings.lightRadius || !context.equals(nextContext)) {
      clear(); world = mc.world; context = nextContext; layer = selected; radius = settings.lightRadius; cx = x; cz = z;
    }
    if (Math.abs((long)x - cx) > 4 || Math.abs((long)z - cz) > 4) {
      cursor = 0; pending.clear(); cx = x; cz = z;
    }
    int width = radius * 2 + 1, total = width * width;
    long start = System.nanoTime();
    while (cursor < total && checksLastTick < 512) {
      if ((checksLastTick & 31) == 0 && System.nanoTime() - start >= 750_000) break;
      int dx = cursor % width - radius, dz = cursor / width - radius;
      cursor++; checksLastTick++;
      if (dx * dx + dz * dz > radius * radius) continue;
      int bx = cx + dx, bz = cz + dz;
      if (MapSampler.loaded(world, bx >> 4, bz >> 4) == null) continue;
      MapTile tile = Maps.cache().peek(MapMath.key(Maps.dimension(), layer, 0, bx, bz));
      int i = MapMath.index(bx, bz);
      if (tile == null || !tile.known(i)) continue;
      // Native support/clearance checks exclude leaves, fluids, slabs and low ceilings.
      // Block light predicts night spawning without changing with full bright or daytime.
      int light = LightOverlay.sample(world, SAMPLE, bx, tile.height(i), bz);
      if (light >= 0 && light < 8) {
        Cell last = pending.isEmpty() ? null : pending.get(pending.size() - 1);
        if (last != null && last.z == bz && last.x + last.length == bx)
          pending.set(pending.size() - 1, new Cell(last.x, bz, last.length + 1));
        else pending.add(new Cell(bx, bz, 1));
      }
    }
    if (++ticks % 4 == 0 && cells.isEmpty() || cursor == total) cells = List.copyOf(pending);
    if (cursor == total) { cursor = 0; pending.clear(); }
  }

  public static void draw(Minecraft mc, MapProjection view, int dimension, boolean circle, boolean mini, MapSettings settings) {
    if (!settings.lightOverlay || world == null || dimension != Maps.dimension() || cells.isEmpty()) return;
    int shown = !mini && mc.currentScreen instanceof WorldMapScreen screen
        ? screen.lightLayer(dimension) : Maps.layer();
    if (shown != layer) return;
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    try {
      GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_CULL_FACE);
      GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glColor4f(.96f, .12f, .08f, .35f);
      GL11.glBegin(GL11.GL_TRIANGLES);
      for (Cell cell : cells) {
        var a = view.screen(cell.x, cell.z); var b = view.screen(cell.x + cell.length, cell.z);
        var c = view.screen(cell.x + cell.length, cell.z + 1); var d = view.screen(cell.x, cell.z + 1);
        var polygon = view.clip(List.of(a, b, c, d), circle);
        for (int i = 1; i + 1 < polygon.size(); i++) {
          GL11.glVertex2d(polygon.get(0).x(), polygon.get(0).y());
          GL11.glVertex2d(polygon.get(i).x(), polygon.get(i).y());
          GL11.glVertex2d(polygon.get(i + 1).x(), polygon.get(i + 1).y());
        }
      }
      GL11.glEnd();
    } finally { GL11.glPopAttrib(); }
  }
}
