package local.luke.power.mixin;

import local.luke.power.render.RenderControls;
import local.luke.power.visual.VisualConfig;
import net.minecraft.class_556;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_556.class)
public class UnderwaterOverlayMixin {
  @Inject(method = "method_1866", at = @At("HEAD"), cancellable = true)
  private void power$underwaterTexture(float delta, CallbackInfo ci) {
    if (VisualConfig.current().removeUnderwaterOverlay || RenderControls.fullBright()) ci.cancel();
  }
}
