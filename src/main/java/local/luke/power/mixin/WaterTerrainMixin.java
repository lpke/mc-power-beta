package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.render.WaterTerrain;
import local.luke.power.render.WaterTerrainSection;
import net.minecraft.class_66;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(class_66.class)
public class WaterTerrainMixin implements WaterTerrainSection {
  @Shadow public boolean field_249;
  @Shadow public boolean[] field_244;
  @Shadow public boolean field_223;
  @Unique private boolean power$waterColumn;
  @Override public boolean power$waterColumn() { return power$waterColumn; }

  @WrapMethod(method = "method_296")
  private void power$lightingColors(Operation<Void> original) {
    if (!field_249) { original.call(); return; }
    power$waterColumn = false;
    if (!WaterTerrain.begin()) { original.call(); return; }
    try {
      original.call();
      power$waterColumn = WaterTerrain.dependsOnWaterColumn();
      if (WaterTerrain.secondPass()) {
        boolean opaqueEmpty = field_244[0], translucentEmpty = field_244[1];
        try { original.call(); }
        finally {
          field_244[0] = opaqueEmpty; field_244[1] = translucentEmpty;
          // Deep water can have zero native skylight yet a daylight-dependent clear colour.
          field_223 = true;
        }
      }
    } finally { WaterTerrain.end(); }
  }
}
