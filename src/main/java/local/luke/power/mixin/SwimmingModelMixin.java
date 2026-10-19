package local.luke.power.mixin;

import local.luke.power.gameplay.*;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class)
public class SwimmingModelMixin implements SwimmingModel {
  @Unique private float power$swimAmount;
  @Unique private boolean power$compact;

  public void power$swimPose(float amount,boolean compact) {
    power$swimAmount=amount;
    power$compact=compact;
  }

  @Inject(method="setAngles",at=@At("HEAD"))
  private void power$noCrouch(float angle,float distance,float age,float yaw,float pitch,float scale,CallbackInfo ci) {
    if(power$compact)((BipedEntityModel)(Object)this).sneaking=false;
  }

  @Inject(method="setAngles",at=@At("TAIL"))
  private void power$stroke(float angle,float distance,float age,float yaw,float pitch,float scale,CallbackInfo ci) {
    float amount=power$swimAmount;
    if(amount<=0)return;
    BipedEntityModel model=(BipedEntityModel)(Object)this;
    if(power$compact)model.head.pitch=SwimAnimation.angle(amount,model.head.pitch,-(float)Math.PI/4);
    model.hat.pitch=model.head.pitch;
    float targetPitch=SwimAnimation.armPitch(angle),leftRoll=SwimAnimation.leftArmRoll(angle);
    float rightAmount=model.handWingProgress>0?0:amount;
    model.leftArm.pitch=SwimAnimation.angle(amount,model.leftArm.pitch,targetPitch);
    model.leftArm.yaw=SwimAnimation.angle(amount,model.leftArm.yaw,(float)Math.PI);
    model.leftArm.roll=SwimAnimation.angle(amount,model.leftArm.roll,leftRoll);
    model.rightArm.pitch=SwimAnimation.mix(rightAmount,model.rightArm.pitch,targetPitch);
    model.rightArm.yaw=SwimAnimation.mix(rightAmount,model.rightArm.yaw,(float)Math.PI);
    model.rightArm.roll=SwimAnimation.mix(rightAmount,model.rightArm.roll,2*(float)Math.PI-leftRoll);
    model.leftLeg.pitch=SwimAnimation.mix(amount,model.leftLeg.pitch,.3f*(float)Math.cos(angle/3+(float)Math.PI));
    model.rightLeg.pitch=SwimAnimation.mix(amount,model.rightLeg.pitch,.3f*(float)Math.cos(angle/3));
  }
}
