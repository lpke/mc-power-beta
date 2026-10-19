package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import local.luke.power.gameplay.*;
import local.luke.power.sprint.SprintController;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Flight replaces travel first. Only the native water branch below is adjusted.
@Mixin(value=LivingEntity.class,priority=900)
public class PlayerWaterTravelMixin {
  @Inject(method="method_937",at=@At(value="INVOKE",target="Lnet/minecraft/entity/LivingEntity;method_942()Z"))
  private void power$settle(CallbackInfo ci) { ModernWater.livingStep((LivingEntity)(Object)this); }

  @WrapOperation(method="method_937",at=@At(value="INVOKE",target="Lnet/minecraft/entity/LivingEntity;method_1334()Z",ordinal=0))
  private boolean power$shallowWaterJump(LivingEntity entity,Operation<Boolean> original) {
    boolean water=original.call(entity);
    return water && !ModernWater.shallowJump(entity);
  }

  @WrapOperation(method="method_937",at=@At(value="INVOKE",target="Lnet/minecraft/entity/LivingEntity;method_944()V"))
  private void power$shallowJumpDelay(LivingEntity entity,Operation<Void> original) {
    if(!ModernWater.shallowJump(entity)) { original.call(entity);return; }
    SwimState state=((SwimPlayer)entity).power$swim();
    if(state.waterJumpTicks==0) { original.call(entity);state.waterJumpTicks=10; }
  }

  @WrapOperation(method="method_945",at=@At(value="INVOKE",target="Lnet/minecraft/entity/LivingEntity;method_1324(FFF)V",ordinal=0))
  private void power$swim(LivingEntity entity,float side,float forward,float acceleration,Operation<Void> original,
      @Share("modernWater") LocalBooleanRef modern){
    modern.set(ModernWater.eligible(entity));
    if(modern.get()){
      var player=(ClientPlayerEntity)entity;
      if(player.field_161!=null && player.field_161.field_2536)player.velocityY-=.04;
      ModernSwimming.steer(player);
    }
    original.call(entity,side,forward,acceleration);
  }
  @ModifyConstant(method="method_945",constant=@Constant(doubleValue=.02,ordinal=0))
  private double power$gravity(double nativeGravity,@Share("modernWater") LocalBooleanRef modern){
    return modern.get()?(SprintController.active((LivingEntity)(Object)this)?0:.005):nativeGravity;
  }

  // Native water multiplies X, Y and Z by the same literal; only X/Z use sprint drag.
  @ModifyConstant(method="method_945",constant={
      @Constant(doubleValue=0.800000011920929,ordinal=0),
      @Constant(doubleValue=0.800000011920929,ordinal=2)})
  private double power$horizontalDrag(double nativeDrag,@Share("modernWater") LocalBooleanRef modern){
    return modern.get() && SprintController.active((LivingEntity)(Object)this) ? (double).9f : nativeDrag;
  }
}
