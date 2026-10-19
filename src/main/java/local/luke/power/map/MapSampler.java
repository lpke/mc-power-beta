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
  private final ArrayDeque<Chunk> queue = new ArrayDeque<>();
  private final LinkedHashSet<Chunk> dirty = new LinkedHashSet<>();
  private final MapPalette palette;
  private World world;
  private Chunk current;
  private int column,
      centreX = Integer.MIN_VALUE,
      centreZ,
      layer = Integer.MIN_VALUE,
      paletteRevision;
  private long ticks;
  public int columnsLastTick;
  public long nanosLastTick;

  public MapSampler(MapPalette palette) {
    this.palette = palette;
  }

  public void invalidate(int x, int z, int maxX, int maxZ) {
    if (centreX == Integer.MIN_VALUE) return;
    int x0 = Math.max(centreX - 16, x >> 4), x1 = Math.min(centreX + 16, maxX >> 4);
    int z0 = Math.max(centreZ - 16, z >> 4), z1 = Math.min(centreZ + 16, maxZ >> 4);
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
    int cx = (int) Math.floor(x) >> 4, cz = (int) Math.floor(z) >> 4;
    dirty.removeIf(c -> Math.abs((long) c.x - cx) > 16 || Math.abs((long) c.z - cz) > 16);
    if (!cache.canSample()) {
      nanosLastTick = System.nanoTime() - start;
      return;
    }
    if (world != mc.world || layer != selectedLayer || paletteRevision != palette.revision()) {
      world = mc.world;
      layer = selectedLayer;
      sampled.clear();
      queue.clear();
      dirty.clear();
      current = null;
      centreX = Integer.MIN_VALUE;
      paletteRevision = palette.revision();
    }
    ticks++;
    if (centreX != cx || centreZ != cz || queue.isEmpty() && current == null) rebuild(cx, cz);
    // Always revisit the camera's chunk on time, even during a long first exploration.
    Chunk focus = new Chunk(cx, cz);
    if (ticks - sampled.getOrDefault(chunkKey(cx, cz), Long.MIN_VALUE / 2)
            >= MapConfig.current().refreshSeconds * 20L
        && !focus.equals(current)) {
      queue.remove(focus);
      queue.addFirst(focus);
    }
    int changes = 0;
    for (var it = dirty.iterator(); it.hasNext() && changes < 16; ) {
      Chunk changed = it.next();
      if (changed.equals(current)) continue;
      it.remove();
      changes++;
      sampled.remove(chunkKey(changed.x, changed.z));
      queue.remove(changed);
      queue.addFirst(changed);
    }
    MapSettings settings = MapConfig.current();
    var touched = new LinkedHashMap<Key, MapTile>();
    int attempts = 0;
    while (columnsLastTick < settings.columnsPerTick && attempts++ < settings.columnsPerTick + 64) {
      if (System.nanoTime() - start >= 1500000L) break;
      if (current == null) {
        current = queue.poll();
        column = 0;
        if (current == null) break;
      }
      class_43 chunk = loaded(world, current.x, current.z);
      if (chunk == null) {
        current = null;
        continue;
      }
      int bx = current.x * 16 + (column & 15), bz = current.z * 16 + (column >> 4);
      MapTile tile = cache.get(key(world.dimension.id, layer, 0, bx, bz));
      if (tile == null) {
        queue.addLast(current);
        current = null;
        continue;
      }
      long before = tile.revision;
      sample(world, chunk, tile, bx, bz, layer < 0 ? 127 : Math.min(127, layer * 8 + 7));
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

  private void rebuild(int cx, int cz) {
    centreX = cx;
    centreZ = cz;
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
    chunks.sort(
        Comparator.comparingLong((Chunk c) -> sampled.containsKey(chunkKey(c.x, c.z)) ? 1 : 0)
            .thenComparingInt(c -> (c.x - cx) * (c.x - cx) + (c.z - cz) * (c.z - cz)));
    queue.addAll(chunks);
  }

  private static long chunkKey(int x, int z) {
    return (long) x << 32 | (z & 0xffffffffL);
  }

  private void sample(World world, class_43 chunk, MapTile tile, int x, int z, int top) {
    int lx = x & 15, lz = z & 15, y = top;
    if (tile.key.layer() >= 0) {
      // Skip the ceiling, then map the first solid floor below a navigable air space.
      while (y > 0 && solid(chunk.method_859(lx, y, lz))) y--;
    }
    while (y > 0 && !visible(chunk.method_859(lx, y, lz))) y--;
    int id = chunk.method_859(lx, y, lz);
    if (id <= 0 || id >= Block.BLOCKS.length || Block.BLOCKS[id] == null) {
      // A surveyed void column must replace its old floor and propagate to distant zooms.
      tile.put(index(x, z), 0, 0, 0, 0, 0, 0);
      return;
    }
    Block block = Block.BLOCKS[id];
    int depth = 0;
    if (block.field_1900 == Material.WATER)
      while (depth < 16
          && y - depth > 0
          && material(chunk.method_859(lx, y - depth, lz)) == Material.WATER) depth++;
    int colour = palette.accurate(block, world, x, y, z, depth),
        nativeColour = MapPalette.nativeColour(block, world, x, y, z);
    int light = world.method_164(class_56.BLOCK, x, Math.min(127, y + 1), z);
    var biome = world.method_1781().method_1787(x, z);
    tile.put(
        index(x, z),
        colour,
        nativeColour,
        y,
        light,
        id,
        biome == null ? 0 : MapPalette.biome(biome.field_888));
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

  public static int layer(Minecraft mc, double x, double y, double z) {
    MapSettings s = MapConfig.current();
    if (s.caveMode == 0) return -1;
    if (s.caveMode == 2) return Math.min(15, s.caveTop / 8);
    int top = Math.max(1, Math.min(127, (int) Math.floor(y) + 2));
    if (mc.world.dimension.id == -1) return top / 8;
    int half = s.roofSize / 2;
    for (int dz = -half; dz <= half; dz++)
      for (int dx = -half; dx <= half; dx++) {
        int px = (int) Math.floor(x) + dx, pz = (int) Math.floor(z) + dz;
        class_43 chunk = loaded(mc.world, px >> 4, pz >> 4);
        if (chunk == null) return -1;
        boolean roof = false;
        for (int py = top; py < 128; py++)
          if (solid(chunk.method_859(px & 15, py, pz & 15))) {
            roof = true;
            break;
          }
        if (!roof) return -1;
      }
    return top / 8;
  }
}
