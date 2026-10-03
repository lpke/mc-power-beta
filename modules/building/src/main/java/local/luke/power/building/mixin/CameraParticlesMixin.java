package local.luke.power.building.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import local.luke.power.building.camera.FreeLook;
import net.minecraft.class_75;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(class_75.class)
public class CameraParticlesMixin {
  @ModifyExpressionValue(
      method = "method_324",
      at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;yaw:F"))
  private float power$yaw(float value) {
    return FreeLook.yaw(value);
  }

  @ModifyExpressionValue(
      method = "method_324",
      at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;pitch:F"))
  private float power$pitch(float value) {
    return FreeLook.pitch(value);
  }
}
