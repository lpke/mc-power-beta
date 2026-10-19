package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.sprint.SprintController;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntity.class, priority = 900)
public class SprintPhysicsMixin {
  @WrapOperation(
      method = "method_945",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/entity/LivingEntity;method_1324(FFF)V",
              ordinal = 2))
  private void power$sprintAcceleration(
      LivingEntity entity,
      float side,
      float forward,
      float acceleration,
      Operation<Void> original) {
    original.call(entity, side, forward, SprintController.acceleration(entity, acceleration));
  }

  @Inject(method = "method_944", at = @At("TAIL"))
  private void power$sprintJump(CallbackInfo ci) {
    SprintController.jumped((LivingEntity) (Object) this);
  }
}
