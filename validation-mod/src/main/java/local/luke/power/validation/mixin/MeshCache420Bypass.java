package local.luke.power.validation.mixin;

import local.luke.power.render.WaterTerrain;
import local.luke.power.validation.MeshCache420Checks;
import net.minecraft.class_43;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fixture-only control, absent from the shipped pack. Both arms use the native renderer. */
@Mixin(value = WaterTerrain.class, remap = false)
public class MeshCache420Bypass {
  @Inject(method = "cacheable", at = @At("HEAD"), cancellable = true)
  private static void power$bypass(class_43 chunk, CallbackInfoReturnable<Boolean> cir) {
    if (MeshCache420Checks.bypass) cir.setReturnValue(false);
  }
}
