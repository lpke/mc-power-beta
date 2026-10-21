package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.render.WaterTerrain;
import local.luke.power.render.WaterTerrainSection;
import local.luke.power.render.HeldLighting;
import net.minecraft.class_66;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_66.class)
public class WaterTerrainMixin implements WaterTerrainSection {
  @Shadow public boolean field_249;
  @Shadow public boolean[] field_244;
  @Shadow public boolean field_223;
  @Shadow public int field_231, field_232, field_233;
  @Shadow public net.minecraft.world.World field_229;
  @Unique private boolean power$waterColumn;
  @Unique private boolean power$heldReady, power$heldRejected;
  @Override public boolean power$waterColumn() { return power$waterColumn; }
  @Override public boolean power$heldLightReady() { return power$heldReady; }
  @Override public boolean power$heldLightRejected() { return power$heldRejected; }

  @Inject(method = "method_298", at = @At("HEAD"))
  private void power$moved(int x, int y, int z, CallbackInfo ci) {
    if (x != field_231 || y != field_232 || z != field_233) power$heldReady = power$heldRejected = false;
  }

  @WrapMethod(method = "method_296")
  private void power$lightingColors(Operation<Void> original) {
    if (!field_249) { original.call(); return; }
    power$waterColumn = false;
    boolean prepared = power$heldReady;
    power$heldReady = power$heldRejected = false;
    WaterTerrain.beginMesh();
    try {
      boolean held = HeldLighting.admit(field_229, field_231, field_232, field_233, prepared);
      if (!WaterTerrain.begin(held)) { original.call(); return; }
      boolean ready = false;
      try {
        original.call();
        power$waterColumn = WaterTerrain.dependsOnWaterColumn();
        boolean opaqueEmpty = field_244[0], translucentEmpty = field_244[1];
        boolean daylight = field_223;
        try {
          if (WaterTerrain.secondPass()) { original.call(); daylight = true; }
          if (WaterTerrain.neutralPass()) original.call();
          ready = WaterTerrain.complete();
        }
        finally {
          field_244[0] = opaqueEmpty; field_244[1] = translucentEmpty;
          // A neutral pass bypasses native daylight queries. Keep the earlier dependency.
          field_223 = daylight;
        }
      } finally { WaterTerrain.end(); }
      power$heldReady = ready;
      power$heldRejected = held && !ready;
    } finally { WaterTerrain.endMesh(); }
  }
}
