package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import local.luke.power.render.*;
import net.minecraft.class_42;
import net.minecraft.class_43;
import net.minecraft.client.Minecraft;

/** Exercises the real render region, flattened chunk and live world callbacks. */
final class TerrainCache420Checks {
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && mc.player != null && !mc.world.isRemote, "open a disposable singleplayer world");
    failures = 0;
    int x = ((int)Math.floor(mc.player.x) >> 4) << 4;
    int z = ((int)Math.floor(mc.player.z) >> 4) << 4;
    int y = 96;
    check(mc.world.method_155(x - 1, y - 1, z - 1, x + 16, y + 16, z + 16), "cache scene requires loaded chunks");
    var region = new class_42(mc.world, x - 1, y - 1, z - 1, x + 16, y + 16, z + 16);
    test("native render-region IDs match every border cell through repeated mesh passes", () -> {
      int[] expected = new int[18 * 18 * 18]; int index = 0;
      for (int by = y - 1; by <= y + 16; by++) for (int bz = z - 1; bz <= z + 16; bz++) for (int bx = x - 1; bx <= x + 16; bx++)
        expected[index++] = region.getBlockId(bx, by, bz);
      WaterTerrain.beginMesh(mc.world, x, y, z);
      try {
        int first = region.getBlockId(x, y, z);
        var cacheField = WaterTerrain.class.getDeclaredField("blockIds"); cacheField.setAccessible(true);
        var cache = (TerrainBlockCache)cacheField.get(null);
        check(cache.get(mc.world, x, y, z) == first, "native render-region call did not populate the mesh cache");
        for (int pass = 0; pass < 3; pass++) {
          index = 0;
          for (int by = y - 1; by <= y + 16; by++) for (int bz = z - 1; bz <= z + 16; bz++) for (int bx = x - 1; bx <= x + 16; bx++)
            check(region.getBlockId(bx, by, bz) == expected[index++], "cached border ID differs from native");
        }
      } finally { WaterTerrain.endMesh(); }
      check(!WaterTerrain.meshing(), "mesh scope leaked");
    });
    test("a native block edit invalidates an active mesh and the next mesh starts fresh", () -> {
      int bx = x + 8, by = y + 8, bz = z + 8;
      int original = mc.world.getBlockId(bx, by, bz), metadata = mc.world.method_1778(bx, by, bz);
      check(mc.world.method_1777(bx, by, bz) == null, "cache edit must not replace a block entity");
      int changed = original == 1 ? 0 : 1;
      WaterTerrain.beginMesh(mc.world, x, y, z);
      try {
        check(region.getBlockId(bx, by, bz) == original, "initial edit cell differs");
        mc.world.method_154(bx, by, bz, changed, 0);
        check(region.getBlockId(bx, by, bz) == changed, "world callback retained stale cached block");
        mc.world.method_200(bx, by, bz, original);
        check(region.getBlockId(bx, by, bz) == original, "raw ID-only setter retained stale cached block");
        boolean outer = WaterTerrain.beginMutation(mc.world);
        try {
          check(WaterTerrain.mutating(), "mutation scope missing");
          check(!WaterTerrain.cacheable(mc.world.method_199(bx >> 4, bz >> 4)), "callback reads still cached");
          boolean inner = WaterTerrain.beginMutation(mc.world);
          try { throw new IllegalStateException("fixture callback"); }
          catch (IllegalStateException expected) { }
          finally { WaterTerrain.endMutation(mc.world, inner); }
          check(WaterTerrain.mutating(), "nested mutation released outer scope");
        } finally { WaterTerrain.endMutation(mc.world, outer); }
        check(!WaterTerrain.mutating(), "mutation scope leaked");
      } finally {
        WaterTerrain.endMesh();
        mc.world.method_154(bx, by, bz, original, metadata);
      }
      WaterTerrain.beginMesh(mc.world, x, y, z);
      try { check(region.getBlockId(bx, by, bz) == original, "new mesh retained previous IDs"); }
      finally { WaterTerrain.endMesh(); }
    });
    test("unknown dynamic chunk subclasses keep every native read", () -> {
      var dynamic = new class_43(mc.world, x >> 4, z >> 4) {
        int calls;
        @Override public int method_859(int bx, int by, int bz) { return ++calls; }
      };
      check(!WaterTerrain.cacheable(dynamic), "custom chunk was admitted to the cache");
      WaterTerrain.beginMesh(mc.world, x, y, z);
      try {
        check(WaterTerrain.block(mc.world, dynamic, x, y, z) == 1, "first dynamic read changed");
        check(WaterTerrain.block(mc.world, dynamic, x, y, z) == 2, "dynamic read was cached");
      } finally { WaterTerrain.endMesh(); }
    });
    log("TERRAIN CACHE 420 FAILURES " + failures);
  }
}
