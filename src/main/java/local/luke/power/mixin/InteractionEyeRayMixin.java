package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import local.luke.power.input.InteractionEye;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Correct only native interaction rays; render-position consumers keep their native origin. */
@Mixin(LivingEntity.class)
public class InteractionEyeRayMixin {
  @ModifyExpressionValue(method="method_929",at=@At(value="INVOKE",
      target="Lnet/minecraft/entity/LivingEntity;method_931(F)Lnet/minecraft/util/math/Vec3d;"))
  private Vec3d power$blockPickOrigin(Vec3d origin,double reach,float delta) {
    return Vec3d.createCached(origin.x,origin.y+InteractionEye.offset(this,delta,0),origin.z);
  }
}
