package local.luke.power.building.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import local.luke.power.building.camera.FreeLook;
import net.minecraft.class_555;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_555.class)
public class CameraRendererMixin {
  @Shadow private Minecraft field_2349;

  @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "method_1844",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;method_2135()V"))
  private void power$focusPause(Minecraft mc, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
    local.luke.power.autowalk.AutoWalk.focusPause(mc, () -> original.call(mc));
  }

  @Inject(method = "method_1844", at = @At("HEAD"))
  private void power$frame(float delta, CallbackInfo ci) {
    FreeLook.update(field_2349);
    local.luke.power.autowalk.InventoryLook.update(field_2349);
  }

  @ModifyExpressionValue(method = "method_1844", at = @At(value = "FIELD",
      target = "Lnet/minecraft/client/Minecraft;field_2778:Z"))
  private boolean power$inventoryLook(boolean focused) {
    return focused || local.luke.power.autowalk.InventoryLook.active() || local.luke.power.autowalk.InventoryLook.hasMotion();
  }

  @Inject(method = "method_1844", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/class_596;method_1972()V", shift = At.Shift.AFTER))
  private void power$quickInventoryLook(float delta, CallbackInfo ci) {
    local.luke.power.autowalk.InventoryLook.supplyMotion(field_2349);
  }

  @ModifyArgs(method = "method_1844", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/client/gui/screen/Screen;render(IIF)V"))
  private void power$hideInventoryPointer(Args args) {
    if (local.luke.power.autowalk.InventoryLook.active()) {
      args.set(0, -10000); args.set(1, -10000);
    }
  }

  @WrapWithCondition(
      method = "method_1844",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/entity/player/ClientPlayerEntity;method_1362(FF)V"))
  private boolean power$turn(ClientPlayerEntity player, float yaw, float pitch) {
    return FreeLook.turn(player, yaw, pitch);
  }

  @ModifyExpressionValue(
      method = "method_1851",
      at = {
        @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;pitch:F"),
        @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;prevPitch:F")
      })
  private float power$pitch(float value) {
    return FreeLook.pitch(value);
  }

  @ModifyExpressionValue(
      method = "method_1851",
      at = {
        @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;yaw:F"),
        @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;prevYaw:F")
      })
  private float power$yaw(float value) {
    return FreeLook.yaw(value);
  }
}
