package local.luke.power.building.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import local.luke.power.building.camera.FreeLook;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderDispatcher.class)
public class CameraLabelsMixin {
  @ModifyExpressionValue(
      method = "method_1917",
      at = {
        @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;yaw:F"),
        @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;prevYaw:F")
      })
  private float power$yaw(float value) {
    return FreeLook.yaw(value);
  }

  @ModifyExpressionValue(
      method = "method_1917",
      at = {
        @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;pitch:F"),
        @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;prevPitch:F")
      })
  private float power$pitch(float value) {
    return FreeLook.pitch(value);
  }
}
