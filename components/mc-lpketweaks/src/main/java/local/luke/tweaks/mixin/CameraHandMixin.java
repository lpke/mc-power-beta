package local.luke.tweaks.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import local.luke.tweaks.camera.FreeLook;
import net.minecraft.class_556;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(class_556.class)
public class CameraHandMixin {
  @ModifyExpressionValue(
      method = "method_1866",
      at = {
        @At(value = "FIELD", target = "Lnet/minecraft/entity/player/ClientPlayerEntity;pitch:F"),
        @At(value = "FIELD", target = "Lnet/minecraft/entity/player/ClientPlayerEntity;prevPitch:F")
      })
  private float lpke$pitch(float value) {
    return FreeLook.pitch(value);
  }

  @ModifyExpressionValue(
      method = "method_1866",
      at = {
        @At(value = "FIELD", target = "Lnet/minecraft/entity/player/ClientPlayerEntity;yaw:F"),
        @At(value = "FIELD", target = "Lnet/minecraft/entity/player/ClientPlayerEntity;prevYaw:F")
      })
  private float lpke$yaw(float value) {
    return FreeLook.yaw(value);
  }
}
