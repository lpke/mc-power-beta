package local.luke.power.mixin;

import local.luke.power.render.RenderControls;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Float shading only, inside the renderer. Simulation light levels never change. */
@Mixin({net.minecraft.world.World.class, net.minecraft.class_42.class})
public class FullBrightLightMixin {
  @Inject(method = {"method_1782", "method_1784"}, at = @At("HEAD"), cancellable = true)
  private void power$light(CallbackInfoReturnable<Float> cir) {
    if (RenderControls.renderingFullBright()) cir.setReturnValue(1f);
  }
}
