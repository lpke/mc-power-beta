package local.luke.power.building.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import local.luke.power.building.camera.FreeLook;
import net.minecraft.class_556;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(class_556.class)
public class CameraVanillaHandMixin {
  @ModifyExpressionValue(
      method = "method_1860",
      at = {
        @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerEntity;pitch:F"),
        @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerEntity;prevPitch:F")
      })
  private float power$pitch(float value) {
    return FreeLook.pitch(value);
  }

  @ModifyExpressionValue(
      method = "method_1860",
      at = {
        @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerEntity;yaw:F"),
        @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerEntity;prevYaw:F")
      })
  private float power$yaw(float value) {
    return FreeLook.yaw(value);
  }
}
