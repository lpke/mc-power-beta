package local.luke.power.mixin;

import local.luke.power.light.LightOverlay;
import net.minecraft.class_555;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_555.class)
public abstract class LightOverlayMixin {
  @Shadow private Minecraft field_2349;

  @Inject(method = "method_1841", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/class_555;method_1847(F)V"))
  private void power$light(float delta, long end, CallbackInfo ci) {
    LightOverlay.render(field_2349, delta);
  }
}
