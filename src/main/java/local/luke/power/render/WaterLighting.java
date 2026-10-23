package local.luke.power.render;

import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.class_43;
import net.minecraft.class_56;
import net.minecraft.world.World;
import net.modificationstation.stationapi.impl.world.chunk.ChunkSection;
import net.modificationstation.stationapi.impl.world.chunk.FlattenedChunk;
import net.modificationstation.stationapi.api.world.HeightLimitView;

/** Read-only daylight compensation. Never changes chunk opacity or either light array. */
public final class WaterLighting {
  private static final WaterColumnCache columns = new WaterColumnCache();
  private static World owner;
  private static boolean preparingColumn;
  private WaterLighting() {}

  public static void clear() { columns.clear(); owner = null; }

  public static float shade(World world, int x, int y, int z, float nativeShade) {
    int darkness = WaterTerrain.buildDarkness(CameraEnvironment.darkness());
    if ((darkness == 100 && !WaterTerrain.detecting()) || !RenderControls.rendering() || world == null
        || world.dimension.field_2177 || y < 0 || y >= 128) return nativeShade;
    // A sample already at the dimension's maximum cannot gain water light, at
    // night or at dawn. Native block/light edits still rebuild it normally.
    if (maximum(world.dimension.field_2178, nativeShade)) return nativeShade;
    if (owner != world) { clear(); owner = world; }
    long key = ((long)x << 32) ^ (z & 0xffffffffL);
    boolean transientColumn = preparingColumn || WaterTerrain.mutating();
    byte[] sky = transientColumn ? null : columns.get(key);
    if (sky == null || y < (sky[128] & 255) || (sky[y] & 128) == 0) {
      int cachedId = WaterTerrain.cachedBlock(world, x, y, z);
      if (cachedId >= 0 && Block.field_1941[cachedId] >= 15) return nativeShade;
      class_43 chunk = CullingTerrain.chunk(world, x >> 4, z >> 4);
      if (chunk == null || !chunk.method_858(x >> 4, z >> 4)) return nativeShade;
      int id = cachedId >= 0 ? cachedId : WaterTerrain.block(world, chunk, x, y, z);
      if (Block.field_1941[id] >= 15) return nativeShade;
      if (sky == null || y < (sky[128] & 255)) {
        int generation = columns.generation();
        if (sky == null) sky = transientColumn ? WaterColumnCache.scratch() : columns.prepare(key);
        boolean nested = preparingColumn;
        preparingColumn = true;
        try { column(chunk, x & 15, z & 15, sky, y); }
        finally { preparingColumn = nested; }
        if (!transientColumn) columns.publish(key, sky, generation);
      }
    }
    // The same frame-local column already knows opacity. Repeated AO samples
    // need neither another chunk-map lookup nor another flattened palette read.
    if ((sky[y] & 192) == 192) return nativeShade;
    // Keep native daylight invalidation even when torchlight currently exceeds the
    // compensated night light. Dawn must be able to rebuild that section later.
    if (WaterTerrain.detecting() && (sky[y] & 16) != 0) class_43.field_953 = true;
    if ((sky[y] & 32) != 0) WaterTerrain.waterColumn();
    int level = Math.max(0, (sky[y] & 15) - world.field_202);
    float clearShade = world.dimension.field_2178[level];
    if (clearShade > nativeShade) WaterTerrain.affected();
    return nativeShade + Math.max(0, clearShade - nativeShade) * (100 - darkness) / 100F;
  }

  static boolean maximum(float[] table, float shade) {
    if (!(shade >= table[15])) return false;
    // Keep custom or live-edited brightness tables correct too.
    for (int i = 0; i < 15; i++) if (!(shade >= table[i])) return false;
    return true;
  }

  private static void column(class_43 chunk, int x, int z, byte[] result, int bottom) {
    int stableOpacity = WaterTerrain.cacheable(chunk) ? 128 : 0;
    if (stableOpacity == 0) bottom = 0;
    int sky = result[129] & 15;
    boolean water = (result[130] & 1) != 0, waterAbove = (result[130] & 2) != 0;
    FlattenedChunk flattened = stableOpacity != 0 && chunk.getClass() == FlattenedChunk.class
        && (((FlattenedChunk)chunk).firstBlock & 15) == 0 && (((FlattenedChunk)chunk).lastBlock & 15) == 15
        ? (FlattenedChunk)chunk : null;
    ChunkSection section = null;
    int sectionY = Integer.MIN_VALUE;
    boolean direct = false;
    for (int y = (result[128] & 255) - 1; y >= bottom; y--) {
      if (flattened != null && sectionY != (y >> 4)) {
        sectionY = y >> 4;
        section = y < flattened.firstBlock || y > flattened.lastBlock ? null
            : flattened.sections[((HeightLimitView)chunk.field_956).sectionCoordToIndex(sectionY)];
        direct = section == null || section.getClass() == ChunkSection.class;
      }
      // The pinned flattened chunk resolves this same section for every ID and
      // light read. Resolve it once per 16 rows, keeping custom sections native.
      int id = direct ? section == null ? 0 : section.getBlockState(x, y & 15, z).block.id
          : chunk.method_859(x, y, z);
      Block block = Block.BLOCKS[id];
      boolean wet = block != null && block.field_1900 == Material.WATER;
      int opacity = wet ? 0 : Block.field_1941[id];
      sky = Math.max(0, sky - opacity);
      if (opacity >= 15) water = false;
      water |= wet;
      waterAbove |= wet;
      int nativeSky = direct ? section == null ? 15 : section.getLight(class_56.SKY, x, y & 15, z)
          : chunk.method_864(class_56.SKY, x, y, z);
      // Native side lighting and roofs remain authoritative. Only recover light
      // lost along a water column, using the dimension's current day/night table.
      sky = Math.max(sky, nativeSky);
      // Track water above roofs too: removing a roof must invalidate the clear color
      // even if the water has already reduced native skylight to zero below it.
      result[y] = (byte)((water ? sky | (sky > nativeSky ? 16 : 0) : nativeSky)
          | (waterAbove ? 32 : 0) | (Block.field_1941[id] >= 15 ? 64 : 0) | stableOpacity);
    }
    result[128] = (byte)bottom; result[129] = (byte)sky;
    result[130] = (byte)((water ? 1 : 0) | (waterAbove ? 2 : 0));
  }
}
