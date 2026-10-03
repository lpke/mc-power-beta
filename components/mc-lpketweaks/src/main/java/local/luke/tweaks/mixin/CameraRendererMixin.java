package local.luke.tweaks.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import local.luke.tweaks.camera.FreeLook;
import net.minecraft.class_555;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_555.class)
public class CameraRendererMixin {
  @Shadow private Minecraft field_2349;

  @Inject(method = "method_1844", at = @At("HEAD"))
  private void lpke$frame(float delta, CallbackInfo ci) {
    FreeLook.update(field_2349);
  }

  @WrapWithCondition(
      method = "method_1844",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/entity/player/ClientPlayerEntity;method_1362(FF)V"))
  private boolean lpke$turn(ClientPlayerEntity player, float yaw, float pitch) {
    return FreeLook.turn(player, yaw, pitch);
  }

  @ModifyExpressionValue(
      method = "method_1851",
      at = {
        @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;pitch:F"),
        @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;prevPitch:F")
      })
  private float lpke$pitch(float value) {
    return FreeLook.pitch(value);
  }

  @ModifyExpressionValue(
      method = "method_1851",
      at = {
        @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;yaw:F"),
        @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;prevYaw:F")
      })
  private float lpke$yaw(float value) {
    return FreeLook.yaw(value);
  }
}
