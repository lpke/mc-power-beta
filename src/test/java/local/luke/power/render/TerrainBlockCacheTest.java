package local.luke.power.render;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class TerrainBlockCacheTest {
  @Test void meshBorderIncludesEveryNativeNeighborWithoutCoordinateAliasing() {
    var cache = new TerrainBlockCache(); var world = new Object(); cache.begin(world, -16, 112, 32);
    int value = 0;
    for (int y = 111; y <= 128; y++) for (int z = 31; z <= 48; z++) for (int x = -17; x <= 0; x++) cache.put(world, x, y, z, value++);
    value = 0;
    for (int y = 111; y <= 128; y++) for (int z = 31; z <= 48; z++) for (int x = -17; x <= 0; x++) assertEquals(value++, cache.get(world, x, y, z));
    for (int[] p : new int[][]{{-18, 112, 32}, {1, 112, 32}, {-16, 110, 32}, {-16, 129, 32}, {-16, 112, 30}, {-16, 112, 49}})
      assertEquals(-1, cache.get(world, p[0], p[1], p[2]));
  }

  @Test void laterMeshesWorldsAndWorkersCannotReuseEarlierBlockIds() throws Exception {
    var cache = new TerrainBlockCache(); var world = new Object(); cache.begin(world, 0, 0, 0); cache.put(world, 0, 0, 0, 0);
    assertEquals(0, cache.get(world, 0, 0, 0)); assertEquals(-1, cache.get(new Object(), 0, 0, 0));
    var result = new AtomicInteger();
    var worker = new Thread(() -> { result.set(cache.get(world, 0, 0, 0)); cache.put(world, 0, 0, 0, 9); cache.end(); });
    worker.start(); worker.join(); assertEquals(-1, result.get()); assertEquals(0, cache.get(world, 0, 0, 0));
    cache.end(); assertEquals(-1, cache.get(world, 0, 0, 0));
    cache.begin(world, 0, 0, 0); assertEquals(-1, cache.get(world, 0, 0, 0));
  }

  @Test void nestedBuildsBypassTheOuterCacheAndLiveEditsInvalidateIt() {
    var cache = new TerrainBlockCache(); var world = new Object(); cache.begin(world, 0, 0, 0); cache.put(world, 1, 2, 3, 4);
    cache.begin(world, 16, 0, 0); assertEquals(-1, cache.get(world, 1, 2, 3)); cache.put(world, 1, 2, 3, 9);
    cache.end(); assertEquals(4, cache.get(world, 1, 2, 3));
    cache.invalidate(world, 18, 0, 0, 20, 16, 16); assertEquals(4, cache.get(world, 1, 2, 3));
    cache.invalidate(world, 1, 2, 3, 1, 2, 3); assertEquals(-1, cache.get(world, 1, 2, 3));
  }
}
