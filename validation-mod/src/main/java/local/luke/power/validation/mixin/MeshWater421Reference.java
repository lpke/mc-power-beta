package local.luke.power.validation.mixin;

import local.luke.power.render.WaterLighting;
import local.luke.power.validation.LegacyWaterLighting421;
import local.luke.power.validation.MeshWater421Checks;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Reference arm exists only in the disposable validation mod. */
@Mixin(value = WaterLighting.class, remap = false)
public class MeshWater421Reference {
  @Inject(method = "clear", at = @At("HEAD"))
  private static void power$clear(CallbackInfo ci) { LegacyWaterLighting421.clear(); }
  @Inject(method = "shade", at = @At("HEAD"), cancellable = true)
  private static void power$reference(World world, int x, int y, int z, float nativeShade, CallbackInfoReturnable<Float> cir) {
    if (MeshWater421Checks.legacy) cir.setReturnValue(LegacyWaterLighting421.shade(world, x, y, z, nativeShade));
  }
}
