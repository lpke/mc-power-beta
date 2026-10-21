package local.luke.power.offhand.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.offhand.*;
import net.minecraft.class_540;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(class_540.class)
public abstract class OffhandFishingRendererMixin extends EntityRenderer {
  @Unique private FishingBobberEntity power$hook;
  @Unique private float power$delta;
  @WrapMethod(method="render(Lnet/minecraft/entity/projectile/FishingBobberEntity;DDDFF)V")
  private void power$owner(FishingBobberEntity hook,double x,double y,double z,float yaw,float delta,Operation<Void> original) {
    FishingBobberEntity old=power$hook;float previousDelta=power$delta;power$hook=hook;power$delta=delta;
    try { original.call(hook,x,y,z,yaw,delta); } finally { power$hook=old;power$delta=previousDelta; }
  }
  @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="render(Lnet/minecraft/entity/projectile/FishingBobberEntity;DDDFF)V",at=@At(value="INVOKE",target="Lnet/minecraft/util/math/Vec3d;createCached(DDD)Lnet/minecraft/util/math/Vec3d;"))
  private Vec3d power$firstPersonTip(double x,double y,double z,Operation<Vec3d> original) {
    if(!power$left()||dispatcher.field_2499.thirdPerson)return original.call(x,y,z);
    var player=power$hook.field_1067;
    var tip=HeldItemTransforms.firstPersonRodTip(-1,OffhandRenderer.swing(player,power$delta),OffhandRenderer.equip(player,power$delta));
    // Native line vectors face +Z and reverse camera X; its existing pitch/yaw rotations remain authoritative.
    return original.call(-tip.x(),tip.y(),-tip.z());
  }
  @ModifyArg(method="render(Lnet/minecraft/entity/projectile/FishingBobberEntity;DDDFF)V",at=@At(value="INVOKE",target="Lnet/minecraft/util/math/Vec3d;rotateY(F)V",ordinal=1),index=0)
  private float power$firstPersonSwing(float angle) { return power$left()?-angle:angle; }
  @ModifyConstant(method="render(Lnet/minecraft/entity/projectile/FishingBobberEntity;DDDFF)V",constant=@Constant(doubleValue=.35))
  private double power$thirdPersonSide(double x) { return power$left()?-x:x; }
  @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="render(Lnet/minecraft/entity/projectile/FishingBobberEntity;DDDFF)V",at=@At(value="INVOKE",target="Lnet/minecraft/entity/player/PlayerEntity;method_930(F)F"))
  private float power$swing(net.minecraft.entity.player.PlayerEntity player,float delta,Operation<Float> original) {
    // The projected tip already includes the complete offhand swing, so native swing rotations stay idle.
    return power$left()&&!dispatcher.field_2499.thirdPerson?0:power$left()?OffhandRenderer.swing(player,delta):original.call(player,delta);
  }
  @Unique private boolean power$left() {
    if(power$hook==null||!Offhand.enabled(power$hook.field_1067))return false;
    var player=power$hook.field_1067;var main=player.inventory.getSelectedItem();var off=Offhand.get(player.inventory);
    return (main==null||!(main.getItem() instanceof net.minecraft.class_607))&&off!=null&&off.getItem() instanceof net.minecraft.class_607;
  }
}
