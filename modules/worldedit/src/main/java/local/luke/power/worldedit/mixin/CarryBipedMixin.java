package local.luke.power.worldedit.mixin;

import local.luke.power.worldedit.carry.CarryPose;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class)
public abstract class CarryBipedMixin implements CarryPose {
  @Unique private boolean power$carrying;

  public void power$carrying(boolean carrying) {
    power$carrying = carrying;
  }

  @Inject(method = "setAngles", at = @At("TAIL"))
  private void power$pose(
      float angle,
      float distance,
      float age,
      float yaw,
      float pitch,
      float scale,
      CallbackInfo ci) {
    if (!power$carrying) return;
    BipedEntityModel model = (BipedEntityModel) (Object) this;
    float bob = (float) Math.cos(angle * .3331f) * distance * .25f - .65f;
    model.rightArm.pitch = bob;
    model.leftArm.pitch = bob - (model.sneaking ? .1f : 0);
    model.rightArm.yaw = model.leftArm.yaw = 0;
    model.rightArm.roll = model.leftArm.roll = 0;
  }
}
