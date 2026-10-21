package local.luke.power.offhand.mixin;

import local.luke.power.offhand.OffhandPose;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class)
public class OffhandModelMixin {
  @Inject(method="setAngles",at=@At("TAIL"))
  private void power$leftSwing(float limb,float stride,float age,float yaw,float pitch,float scale,CallbackInfo ci) {
    OffhandPose.apply((BipedEntityModel)(Object)this);
  }
}
