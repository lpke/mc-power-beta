package local.luke.power.map;

import static local.luke.power.map.MapMath.*;

import java.util.*;
import local.luke.power.render.CullingTerrain;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.class_43;
import net.minecraft.class_56;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

/** Incremental native sampling. Never asks a chunk provider to load or generate terrain. */
public final class MapSampler {
  private record Chunk(int x, int z) {}

  private final LinkedHashMap<Long, Long> sampled = new LinkedHashMap<>();
  private final Map<Long, Integer> progress = new HashMap<>();
  private final ArrayDeque<Chunk> queue = new ArrayDeque<>();
  private final LinkedHashSet<Chunk> dirty = new LinkedHashSet<>();
  private final MapPalette palette;
  private final MapCadence cadence = new MapCadence();
  private World world;
  private String context = "";
  private Chunk current;
  private MapSamplingFocus focus;
  private int column,
      centerX = Integer.MIN_VALUE,
      centerZ,
      layer = Integer.MIN_VALUE,
      paletteRevision;
  private long ticks;
  private long lastRebuild;
  public int columnsLastTick;
  public long nanosLastTick;

  public MapSampler(MapPalette palette) {
    this.palette = palette;
  }

  public void invalidate(int x, int z, int maxX, int maxZ) {
    if (centerX == Integer.MIN_VALUE) return;
    int x0 = Math.max(centerX - 16, x >> 4), x1 = Math.min(centerX + 16, maxX >> 4);
    int z0 = Math.max(centerZ - 16, z >> 4), z1 = Math.min(centerZ + 16, maxZ >> 4);
    for (int cx = x0; cx <= x1; cx++)
      for (int cz = z0; cz <= z1; cz++) {
        if (dirty.size() >= 1089) return;
        dirty.add(new Chunk(cx, cz));
      }
  }

  public static class_43 loaded(World world, int cx, int cz) {
    class_43 chunk = CullingTerrain.chunk(world, cx, cz);
    return chunk != null && chunk.method_858(cx, cz) && world.method_239(cx * 16, 64, cz * 16)
        ? chunk
        : null;
  }

  public void tick(Minecraft mc, MapCache cache, int selectedLayer, double x, double y, double z) {
    long start = System.nanoTime();
    columnsLastTick = 0;
    MapSettings settings = MapConfig.current();
    CaveWindow window = selectedLayer < 0 ? null : window(settings, y);
    String selectedContext = window == null ? "" : window.context();
    cache.viewContext(window(settings, y).context());
    if (world != mc.world || layer != selectedLayer || !context.equals(selectedContext)
        || paletteRevision != palette.revision()) {
      world = mc.world;
      layer = selectedLayer;
      context = selectedContext;
      sampled.clear();
      progress.clear();
      queue.clear();
      dirty.clear();
      current = null;
      centerX = Integer.MIN_VALUE;
      paletteRevision = palette.revision();
    }
    // Refresh seconds use the game clock, independently of sampling rate and backpressure.
    ticks++;
    if (!cadence.tick(settings.updateRate)) {
      nanosLastTick = 0;
      return;
    }
    int cx = (int) Math.floor(x) >> 4, cz = (int) Math.floor(z) >> 4;
    dirty.removeIf(c -> Math.abs((long) c.x - cx) > 16 || Math.abs((long) c.z - cz) > 16);
    progress.keySet().removeIf(key -> Math.abs((long)(int)(key >> 32) - cx) > 16
        || Math.abs((long)key.intValue() - cz) > 16);
    if (!cache.canSample()) {
      nanosLastTick = System.nanoTime() - start;
      return;
    }
    var mapView = mc.currentScreen instanceof local.luke.power.ui.WorldMapScreen screen
        ? screen.samplingView(world.dimension.id, selectedLayer) : null;
    int radius = Math.min(16, local.luke.power.controls.util.ModOptions.getRenderDistanceChunks());
    var nextFocus = MapSamplingFocus.of(mapView, cx, cz, radius);
    if (centerX != cx || centerZ != cz || !nextFocus.equals(focus)
        || queue.isEmpty() && current == null || mapView != null && ticks - lastRebuild >= 20) {
      focus = nextFocus;
      lastRebuild = ticks;
      rebuild(cx, cz);
    }
    boolean admitted = false;
    // Live nearby terrain must refresh even while the peripheral survey is unfinished.
    for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) {
      var near = new Chunk(cx + dx, cz + dz);
      if (!near.equals(current) && due(near) && !queue.contains(near)
          && loaded(world, near.x, near.z) != null) {
        queue.addLast(near); admitted = true;
      }
    }
    int changes = 0;
    for (var it = dirty.iterator(); it.hasNext() && changes < 16; ) {
      Chunk changed = it.next();
      if (changed.equals(current)) continue;
      it.remove();
      changes++;
      sampled.remove(chunkKey(changed.x, changed.z));
      progress.remove(chunkKey(changed.x, changed.z));
      queue.remove(changed);
      queue.addFirst(changed);
    }
    if (changes > 0 || admitted) {
      order(cx, cz);
      if (current != null && !queue.isEmpty() && priority(cx, cz).compare(queue.peek(), current) < 0) {
        park(); order(cx, cz);
      }
    }
    boolean mapOpen = mc.currentScreen instanceof local.luke.power.ui.WorldMapScreen;
    int maxColumns = mapOpen ? settings.mapOpenColumns : settings.columnsPerTick;
    long timeBudget = mapOpen ? (long)(settings.mapOpenBudget * 1_000_000) : 1_500_000L;
    var touched = new LinkedHashMap<Key, MapTile>();
    int attempts = 0;
    while (columnsLastTick < maxColumns && attempts++ < maxColumns + 64) {
      if (System.nanoTime() - start >= timeBudget) break;
      if (current == null) {
        current = queue.poll();
        if (current == null) break;
        column = progress.getOrDefault(chunkKey(current.x, current.z), 0);
        progress.remove(chunkKey(current.x, current.z));
      }
      class_43 chunk = loaded(world, current.x, current.z);
      if (chunk == null) {
        progress.remove(chunkKey(current.x, current.z));
        current = null;
        continue;
      }
      int bx = current.x * 16 + (column & 15), bz = current.z * 16 + (column >> 4);
      MapTile tile = cache.sample(key(world.dimension.id, layer, 0, bx, bz, context));
      if (tile == null) {
        park();
        continue;
      }
      long before = tile.revision;
      sample(world, chunk, tile, bx, bz, window);
      if (tile.revision != before) touched.put(tile.key, tile);
      columnsLastTick++;
      column++;
      if (column == 256) {
        sampled.put(chunkKey(current.x, current.z), ticks);
        current = null;
      }
    }
    touched.values().forEach(cache::changed);
    while (sampled.size() > 4096) sampled.remove(sampled.keySet().iterator().next());
    nanosLastTick = System.nanoTime() - start;
  }

  private boolean due(Chunk chunk) {
    return ticks - sampled.getOrDefault(chunkKey(chunk.x, chunk.z), Long.MIN_VALUE / 2)
        >= MapConfig.current().refreshSeconds * 20L;
  }

  private void park() {
    if (column > 0) progress.put(chunkKey(current.x, current.z), column);
    queue.remove(current);
    queue.addLast(current);
    current = null;
  }

  private void rebuild(int cx, int cz) {
    centerX = cx;
    centerZ = cz;
    if (current != null && column > 0) progress.put(chunkKey(current.x, current.z), column);
    queue.clear();
    current = null;
    int radius = Math.min(16, local.luke.power.controls.util.ModOptions.getRenderDistanceChunks());
    var chunks = new ArrayList<Chunk>();
    for (int z = -radius; z <= radius; z++)
      for (int x = -radius; x <= radius; x++) {
        int px = cx + x, pz = cz + z;
        long last = sampled.getOrDefault(chunkKey(px, pz), Long.MIN_VALUE / 2);
        if (ticks - last >= MapConfig.current().refreshSeconds * 20L
            && loaded(world, px, pz) != null) chunks.add(new Chunk(px, pz));
      }
    queue.addAll(chunks);
    order(cx, cz);
  }

  private void order(int cx, int cz) {
    var chunks = new ArrayList<>(queue);
    chunks.sort(priority(cx, cz));
    queue.clear(); queue.addAll(chunks);
  }

  private Comparator<Chunk> priority(int cx, int cz) {
    return Comparator.comparingInt((Chunk c) -> focus.tier(c.x, c.z, cx, cz,
        sampled.containsKey(chunkKey(c.x, c.z))))
        .thenComparingLong(c -> sampled.getOrDefault(chunkKey(c.x, c.z), Long.MIN_VALUE / 2))
        .thenComparingLong(c -> focus.distance(c.x, c.z, cx, cz));
  }

  private static long chunkKey(int x, int z) {
    return (long) x << 32 | (z & 0xffffffffL);
  }

  private void sample(World world, class_43 chunk, MapTile tile, int x, int z, CaveWindow window) {
    int lx = x & 15, lz = z & 15;
    int bottom = window == null ? 0 : window.bottom();
    int y = window == null ? 127 : window.floor(
        py -> solid(chunk.method_859(lx, py, lz)),
        py -> visible(chunk.method_859(lx, py, lz)));
    y = CaveWindow.descend(y, bottom, py -> !visible(chunk.method_859(lx, py, lz)));
    if (y < bottom) {
      tile.put(index(x, z), 0, 0, 0, 0, 0, 0);
      return;
    }
    int id = chunk.method_859(lx, y, lz);
    if (id <= 0 || id >= Block.BLOCKS.length || Block.BLOCKS[id] == null) {
      // A surveyed void column must replace its old floor and propagate to distant zooms.
      tile.put(index(x, z), 0, 0, 0, 0, 0, 0);
      return;
    }
    int surfaceY = y;
    // Thin snow contributes its white pigment without raising the terrain by a
    // whole block. The full snow block keeps its actual height.
    int terrainY = id == 78 ? Math.max(bottom, y - 1) : y;
    double r = 0, g = 0, b = 0, nr = 0, ng = 0, nb = 0, remaining = 1;
    int waterOpacity = 0;
    // Descend only within this already-loaded column. Both the 128-block world
    // height, selected cave window and eight transparent surfaces bound the extra work.
    for (int surfaces = 0; surfaces < 8 && y >= bottom && remaining > .01; surfaces++) {
      y = CaveWindow.descend(y, bottom, py -> !visible(chunk.method_859(lx, py, lz)));
      if (y < bottom) break;
      int layerId = chunk.method_859(lx, y, lz);
      Block surface = Block.BLOCKS[layerId];
      int color = palette.surfaceColor(surface, world, x, y, z);
      int nativeColor = MapPalette.nativeColor(surface, world, x, y, z);
      double alpha = (color >>> 24) / 255.0;
      double lightThroughWater = (9 + Math.max(0, 15 - waterOpacity)) / 24.0;
      double weight = remaining * alpha * lightThroughWater;
      r += (color >> 16 & 255) * weight;
      g += (color >> 8 & 255) * weight;
      b += (color & 255) * weight;
      nr += (nativeColor >> 16 & 255) * weight;
      ng += (nativeColor >> 8 & 255) * weight;
      nb += (nativeColor & 255) * weight;
      remaining *= 1 - alpha;
      if (remaining <= .01) break;
      // A continuous body of water has one top surface. Its depth attenuates
      // light reaching the seabed instead of repeatedly painting opaque blue.
      int bodyTop = y;
      y = CaveWindow.descend(y - 1, bottom, py -> chunk.method_859(lx, py, lz) == layerId);
      int depth = bodyTop - y;
      if (surface.field_1900 == Material.WATER || layerId == 79)
        waterOpacity = Math.min(15, waterOpacity + depth * 3);
    }
    int color = rgb(r, g, b), nativeColor = rgb(nr, ng, nb);
    int light = world.method_164(class_56.BLOCK, x, Math.min(127, surfaceY + 1), z);
    var biome = world.method_1781().method_1787(x, z);
    tile.put(
        index(x, z),
        color,
        nativeColor,
        terrainY,
        light,
        id,
        biome == null ? 0 : MapPalette.biome(biome.field_888));
  }

  private static int rgb(double r, double g, double b) {
    return (int) Math.min(255, r) << 16 | (int) Math.min(255, g) << 8 | (int) Math.min(255, b);
  }

  private static Material material(int id) {
    return id > 0 && id < Block.BLOCKS.length && Block.BLOCKS[id] != null
        ? Block.BLOCKS[id].field_1900
        : Material.AIR;
  }

  private static boolean solid(int id) {
    return id > 0
        && id < Block.BLOCKS.length
        && Block.BLOCKS[id] != null
        && Block.BLOCKS[id].method_1623();
  }

  private static boolean visible(int id) {
    Material m = material(id);
    return m != Material.AIR
        && (m.method_905()
            || m == Material.WATER
            || m == Material.LAVA
            || m == Material.LEAVES
            || id == 78);
  }

  public static CaveWindow window(MapSettings settings, double cameraY) {
    return CaveWindow.at(settings.caveMode == 2 ? settings.caveTop : cameraY, settings.caveAbove, settings.caveBelow);
  }

  public static int layer(Minecraft mc, double x, double y, double z) {
    MapSettings s = MapConfig.current();
    if (s.caveMode == 0) return -1;
    CaveWindow window = window(s, y);
    if (s.caveMode == 2) return window.layer();
    int top = window.top();
    if (mc.world.dimension.id == -1) return window.layer();
    int half = s.roofSize / 2;
    for (int dz = -half; dz <= half; dz++)
      for (int dx = -half; dx <= half; dx++) {
        int px = (int) Math.floor(x) + dx, pz = (int) Math.floor(z) + dz;
        class_43 chunk = loaded(mc.world, px >> 4, pz >> 4);
        if (chunk == null) return -1;
        boolean roof = false;
        for (int py = top; py < 128; py++) {
          int id = chunk.method_859(px & 15, py, pz & 15);
          // Leaves are full render cubes in both native graphics modes, but a
          // canopy must not turn outdoor terrain into a dark cave layer.
          if (material(id) != Material.LEAVES && solid(id)) {
            roof = true;
            break;
          }
        }
        if (!roof) return -1;
      }
    return window.layer();
  }
}
