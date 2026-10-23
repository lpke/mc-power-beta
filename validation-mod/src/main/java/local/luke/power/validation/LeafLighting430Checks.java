package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.Proxy;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.world.BlockView;

/** Exercise the transformed native block method without editing terrain or settings files. */
final class LeafLighting430Checks {
  static void run() throws Exception {
    Object config = Class.forName("local.luke.power.controls.ControlFeatures").getField("TWEAKS_CONFIG").get(null);
    var enabled = config.getClass().getField("beta18LeavesRendering");
    Object previous = enabled.get(config);
    var brightness = Class.forName("net.modificationstation.stationapi.impl.block.BlockBrightness").getField("light");
    int originalBrightness = brightness.getInt(null);
    int checked = 0;
    try {
      for (boolean on : new boolean[]{false, true}) {
        enabled.set(config, on);
        for (int stateLight : new int[]{0, 7, 15}) {
        brightness.setInt(null, stateLight);
        for (Material material : new Material[]{Material.LEAVES, Material.STONE, Material.WATER})
          for (int id : new int[]{1, 18, 89})
            for (float shade : new float[]{0, .1F, .2F, .7F, 1, -0.0F, Float.NaN}) {
              int[] reads = {0};
              BlockView view = (BlockView)Proxy.newProxyInstance(BlockView.class.getClassLoader(),
                  new Class<?>[]{BlockView.class}, (proxy, method, args) -> {
                    if (method.getName().equals("method_1779")) return material;
                    if (method.getName().equals("method_1784")) {
                      check((int)args[0] == 12 && (int)args[1] == 64 && (int)args[2] == 34
                          && (int)args[3] == (on && material == Material.LEAVES ? Block.field_1943[id] : stateLight),
                          "native brightness arguments changed, emission=" + args[3] + " id=" + id);
                      reads[0]++; return shade;
                    }
                    throw new AssertionError("Unexpected block-view query " + method.getName());
                  });
              float actual = Block.BLOCKS[id].method_1604(view, 12, 64, 34);
              float expected = on && material == Material.LEAVES ? Math.min(shade, .2F) : shade;
              check(Float.floatToIntBits(actual) == Float.floatToIntBits(expected), "leaf shade differs");
              check(reads[0] == 1, "brightness must be sampled exactly once");
              checked++;
            }
        }
      }
      log("LEAF LIGHTING 430 PASS " + checked + " exact native results, one brightness read each");
    } finally { enabled.set(config, previous); brightness.setInt(null, originalBrightness); }
  }
}
