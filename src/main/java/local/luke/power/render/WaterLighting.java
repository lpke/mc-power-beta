package local.luke.power.render;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.class_43;
import net.minecraft.class_56;
import net.minecraft.world.World;

/** Read-only daylight compensation. Never changes chunk opacity or either light array. */
public final class WaterLighting {
  private static final Map<Long, byte[]> columns = new HashMap<>();
  private static World owner;
  private WaterLighting() {}

  public static void clear() { columns.clear(); owner = null; }

  public static float shade(World world, int x, int y, int z, float nativeShade) {
    int darkness = WaterTerrain.buildDarkness(CameraEnvironment.darkness());
    if ((darkness == 100 && !WaterTerrain.detecting()) || !RenderControls.rendering() || world == null
        || world.dimension.field_2177 || y < 0 || y >= 128) return nativeShade;
    if (owner != world) { clear(); owner = world; }
    class_43 chunk = CullingTerrain.chunk(world, x >> 4, z >> 4);
    if (chunk == null || !chunk.method_858(x >> 4, z >> 4)) return nativeShade;
    int id = chunk.method_859(x & 15, y, z & 15);
    // Preserve opaque ambient-occlusion samples, including submerged walls/floors.
    if (Block.field_1941[id] >= 15) return nativeShade;
    long key = ((long)x << 32) ^ (z & 0xffffffffL);
    byte[] sky = columns.get(key);
    if (sky == null) {
      sky = column(chunk, x & 15, z & 15);
      // Bound transient memory even in very large isometric captures.
      if (columns.size() < 4096) columns.put(key, sky);
    }
    // Keep native daylight invalidation even when torchlight currently exceeds the
    // compensated night light. Dawn must be able to rebuild that section later.
    if (WaterTerrain.detecting() && (sky[y] & 16) != 0) class_43.field_953 = true;
    if ((sky[y] & 32) != 0) WaterTerrain.waterColumn();
    int level = Math.max(0, (sky[y] & 15) - world.field_202);
    float clearShade = world.dimension.field_2178[level];
    if (clearShade > nativeShade) WaterTerrain.affected();
    return nativeShade + Math.max(0, clearShade - nativeShade) * (100 - darkness) / 100F;
  }

  private static byte[] column(class_43 chunk, int x, int z) {
    byte[] result = new byte[128];
    int sky = 15;
    boolean water = false, waterAbove = false;
    for (int y = 127; y >= 0; y--) {
      int id = chunk.method_859(x, y, z);
      Block block = Block.BLOCKS[id];
      boolean wet = block != null && block.field_1900 == Material.WATER;
      int opacity = wet ? 0 : Block.field_1941[id];
      sky = Math.max(0, sky - opacity);
      if (opacity >= 15) water = false;
      water |= wet;
      waterAbove |= wet;
      int nativeSky = chunk.method_864(class_56.SKY, x, y, z);
      // Native side lighting and roofs remain authoritative. Only recover light
      // lost along a water column, using the dimension's current day/night table.
      sky = Math.max(sky, nativeSky);
      // Track water above roofs too: removing a roof must invalidate the clear colour
      // even if the water has already reduced native skylight to zero below it.
      result[y] = (byte)((water ? sky | (sky > nativeSky ? 16 : 0) : nativeSky) | (waterAbove ? 32 : 0));
    }
    return result;
  }
}
