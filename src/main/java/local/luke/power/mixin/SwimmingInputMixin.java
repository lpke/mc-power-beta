package local.luke.power.mixin;

import local.luke.power.gameplay.*;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(ClientPlayerEntity.class)
public class SwimmingInputMixin {
  @Inject(method="method_937", at=@At(value="INVOKE", ordinal=0,
      target="Lnet/minecraft/entity/player/ClientPlayerEntity;method_1372(DDD)Z"))
  private void power$preparePose(CallbackInfo ci) {
    ModernSwimming.preparePose((ClientPlayerEntity)(Object)this);
  }

  @Inject(method="method_1372",at=@At("HEAD"),cancellable=true)
  private void power$shortCollision(double x,double y,double z,CallbackInfoReturnable<Boolean> cir) {
    if (ModernSwimming.lowPose((ClientPlayerEntity)(Object)this)) cir.setReturnValue(false);
  }

  @Inject(method="method_1373",at=@At("HEAD"),cancellable=true)
  private void power$blockedStand(CallbackInfoReturnable<Boolean> cir) {
    if (ModernSwimming.crouched((ClientPlayerEntity)(Object)this)) cir.setReturnValue(true);
  }
}
