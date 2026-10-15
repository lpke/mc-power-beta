package local.luke.power.validation.mixin;

import local.luke.power.validation.FreecamMotionChecks;
import net.minecraft.class_555;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_555.class)
public class FreecamMotionCapture {
  @Shadow private Minecraft field_2349;
  @Inject(method = "method_1841", at = @At("HEAD"))
  private void begin(float delta, long deadline, CallbackInfo ci) { FreecamMotionChecks.beginFrame(); }
  @Inject(method = "method_1844", at = @At("TAIL"))
  private void capture(float delta, CallbackInfo ci) { FreecamMotionChecks.frame(field_2349); }
}
