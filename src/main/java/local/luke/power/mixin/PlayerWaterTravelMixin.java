package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import local.luke.power.gameplay.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

// Flight replaces travel first. Only the native water branch below is adjusted.
@Mixin(value=LivingEntity.class,priority=900)
public class PlayerWaterTravelMixin {
  @WrapOperation(method="method_945",at=@At(value="INVOKE",target="Lnet/minecraft/entity/LivingEntity;method_1324(FFF)V",ordinal=0))
  private void power$swim(LivingEntity entity,float side,float forward,float acceleration,Operation<Void> original,
      @Share("modernWater") LocalBooleanRef modern){
    modern.set(ModernWater.eligible(entity));
    if(modern.get()){
      var player=(ClientPlayerEntity)entity;
      if(player.field_161!=null && player.field_161.field_2536)player.velocityY-=.04;
    }
    original.call(entity,side,forward,acceleration);
  }
  @ModifyConstant(method="method_945",constant=@Constant(doubleValue=.02,ordinal=0))
  private double power$gravity(double nativeGravity,@Share("modernWater") LocalBooleanRef modern){
    return modern.get()?.005:nativeGravity;
  }
}
