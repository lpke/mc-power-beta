package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.sprint.SprintController;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntity.class, priority = 900)
public class SprintPhysicsMixin {
  @ModifyConstant(method = "method_945", constant = @Constant(doubleValue = .2))
  private double power$ladderSprint(double speed) {
    return SprintController.ladderSpeed((LivingEntity) (Object) this, speed);
  }

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

  @WrapOperation(method="method_945",at=@At(value="INVOKE",
      target="Lnet/minecraft/entity/LivingEntity;move(DDD)V"))
  private void power$collision(LivingEntity entity,double x,double y,double z,Operation<Void> original) {
    double startX=entity.x,startZ=entity.z;
    original.call(entity,x,y,z);
    SprintController.moved(entity,entity.x-startX,entity.z-startZ);
  }

  @Inject(method = "method_944", at = @At("TAIL"))
  private void power$sprintJump(CallbackInfo ci) {
    SprintController.jumped((LivingEntity) (Object) this);
  }
}
