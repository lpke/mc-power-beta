package local.luke.tweaks.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import local.luke.tweaks.camera.FreeLook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(
    targets =
        "net.modificationstation.stationapi.impl.client.arsenic.renderer.render.ArsenicOverlayRenderer",
    remap = false)
public class CameraArsenicHandMixin {
  @ModifyExpressionValue(
      method = "renderItem(F)V",
      remap = false,
      at = {
        @At(
            value = "FIELD",
            remap = true,
            target = "Lnet/minecraft/entity/player/ClientPlayerEntity;pitch:F"),
        @At(
            value = "FIELD",
            remap = true,
            target = "Lnet/minecraft/entity/player/ClientPlayerEntity;prevPitch:F")
      })
  private float lpke$pitch(float value) {
    return FreeLook.pitch(value);
  }

  @ModifyExpressionValue(
      method = "renderItem(F)V",
      remap = false,
      at = {
        @At(
            value = "FIELD",
            remap = true,
            target = "Lnet/minecraft/entity/player/ClientPlayerEntity;yaw:F"),
        @At(
            value = "FIELD",
            remap = true,
            target = "Lnet/minecraft/entity/player/ClientPlayerEntity;prevYaw:F")
      })
  private float lpke$yaw(float value) {
    return FreeLook.yaw(value);
  }
}
