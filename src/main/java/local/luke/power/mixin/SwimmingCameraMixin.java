package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import local.luke.power.gameplay.ModernSwimming;
import local.luke.power.input.InteractionEye;
import net.minecraft.class_555;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

@Mixin(class_555.class)
public class SwimmingCameraMixin {
  @Shadow private Minecraft field_2349;

  @ModifyExpressionValue(method="method_1851",at=@At(value="FIELD",
      target="Lnet/minecraft/entity/LivingEntity;field_1631:F"))
  private float power$eyeTransition(float offset,float delta) {
    return ModernSwimming.cameraOffset(field_2349.field_2807,delta,offset);
  }

  @ModifyExpressionValue(method="method_1838",at=@At(value="INVOKE",
      target="Lnet/minecraft/entity/LivingEntity;method_931(F)Lnet/minecraft/util/math/Vec3d;"))
  private Vec3d power$entityPickOrigin(Vec3d origin,float delta) {
    return Vec3d.createCached(origin.x,origin.y+InteractionEye.offset(field_2349.field_2807,delta,0),origin.z);
  }

  @ModifyArgs(method="method_1838",at=@At(value="INVOKE",
      target="Lnet/minecraft/world/World;getEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;)Ljava/util/List;"))
  private void power$entityPickBounds(Args args,float delta) {
    double offset=InteractionEye.offset(args.get(0),delta,0);
    if (offset==0) return;
    Box bounds=args.get(1);
    // Retain the native query and admit its shifted eye ray during compact-pose easing.
    args.set(1,Box.getOrCreate(bounds.minX,bounds.minY+Math.min(0,offset),bounds.minZ,
        bounds.maxX,bounds.maxY+Math.max(0,offset),bounds.maxZ));
  }
}
