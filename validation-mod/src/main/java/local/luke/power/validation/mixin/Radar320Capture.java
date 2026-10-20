package local.luke.power.validation.mixin;

import local.luke.power.validation.Radar320Checks;
import net.minecraft.class_555;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_555.class)
public abstract class Radar320Capture {
  @Shadow private Minecraft field_2349;
  @Inject(method = "method_1844", at = @At("HEAD"))
  private void power$observe(float delta, CallbackInfo ci) {
    Radar320Checks.observe(field_2349);
  }
  @Inject(method = "method_1844", at = @At("TAIL"))
  private void power$frame(float delta, CallbackInfo ci) {
    Radar320Checks.frame(field_2349);
  }
}
