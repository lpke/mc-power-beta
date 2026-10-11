package local.luke.power.render;

import local.luke.power.mixin.CullingChunkCacheAccessor;
import local.luke.power.mixin.CullingArrayCacheAccessor;
import net.minecraft.class_43;
import net.minecraft.class_455;
import net.minecraft.class_515;
import net.minecraft.world.World;

/** The culling worker must never invoke the local chunk loader, even for loaded chunks. */
public final class CullingTerrain {
  private CullingTerrain() {}
  public static boolean supported(World world) {
    var source = world.method_259();
    return source instanceof CullingChunkCacheAccessor || source instanceof CullingArrayCacheAccessor || source instanceof class_455;
  }
  public static int block(World world, int x, int y, int z) {
    if (y < 0 || y >= 128) return 0;
    class_43 chunk = chunk(world, x >> 4, z >> 4);
    return chunk == null ? 0 : chunk.method_859(x & 15, y, z & 15);
  }
  public static class_43 chunk(World world, int x, int z) {
    if (world.method_259() instanceof CullingChunkCacheAccessor cache) {
      class_43 chunk = cache.power$loadedChunks().get(class_515.method_1854(x, z));
      return chunk == null ? cache.power$emptyChunk() : chunk;
    }
    if (world.method_259() instanceof CullingArrayCacheAccessor cache) {
      class_43 chunk = cache.power$loadedChunks()[(x & 31) + (z & 31) * 32];
      return chunk != null && chunk.method_858(x, z) ? chunk : cache.power$emptyChunk();
    }
    // Multiplayer's method_1806 only reads received chunks and supplies empty placeholders.
    if (world.method_259() instanceof class_455 source) return source.method_1806(x, z);
    return null;
  }
}
