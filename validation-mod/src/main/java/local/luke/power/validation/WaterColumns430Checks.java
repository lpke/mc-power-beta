package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.*;
import local.luke.power.render.*;
import local.luke.power.visual.VisualConfig;
import net.minecraft.client.Minecraft;

/** Checks lazy column extensions against frozen 4.2.3 in several read orders. */
final class WaterColumns430Checks {
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && mc.player != null, "open the disposable world");
    var visual = VisualConfig.copy(); int sky = mc.world.field_202, checks = 0;
    var settings = VisualConfig.copy();
    settings.underwaterDarkness = settings.underwaterDarknessOutside = 0;
    try {
      VisualConfig.preview(settings); CameraEnvironment.updateLighting(mc);
      RenderControls.beginRender(false);
      var order = new ArrayList<Integer>();
      for (int y = 0; y < 128; y++) order.add(y);
      for (int darkness : new int[]{0, 4, 11}) {
        mc.world.field_202 = darkness;
        for (int traversal = 0; traversal < 3; traversal++) {
          if (traversal == 1) Collections.reverse(order);
          if (traversal == 2) Collections.shuffle(order, new Random(430));
          WaterLighting.clear(); LegacyWaterLighting430.clear();
          for (int offset = -8; offset <= 8; offset += 4) for (int y : order)
            for (float nativeShade : new float[]{0, .125F, .6F, 1}) {
              int x = (int)Math.floor(mc.player.x) + offset, z = (int)Math.floor(mc.player.z) - offset;
              float expected = LegacyWaterLighting430.shade(mc.world, x, y, z, nativeShade);
              float actual = WaterLighting.shade(mc.world, x, y, z, nativeShade);
              check(Float.floatToIntBits(expected) == Float.floatToIntBits(actual),
                  "water column differs at " + x + "," + y + "," + z + " sky=" + darkness);
              checks++;
            }
        }
      }
      log("WATER COLUMNS 430 PASS exact float bits for " + checks + " native terrain/light queries");
    } finally {
      RenderControls.endRender(); mc.world.field_202 = sky;
      VisualConfig.preview(visual); CameraEnvironment.updateLighting(mc); WaterLighting.clear();
    }
  }
}
