package local.luke.power.validation.mixin;

import local.luke.power.validation.CameraJitter470Checks;
import net.minecraft.class_555;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_555.class)
public class CameraJitter470Capture {
  @Shadow private Minecraft field_2349;
  @Inject(method="method_1844",at=@At("TAIL"))
  private void power$recordFrame(float delta,CallbackInfo ci) {
    CameraJitter470Checks.frame(field_2349,delta);
  }
  @Inject(method="method_1841",at=@At("HEAD"))
  private void power$aim(float delta,long deadline,CallbackInfo ci) {
    CameraJitter470Checks.aim(field_2349,delta);
  }
  @Inject(method="method_1840",at=@At("RETURN"))
  private void power$camera(float delta,int eye,CallbackInfo ci) {
    CameraJitter470Checks.camera(field_2349,delta);
  }
}
