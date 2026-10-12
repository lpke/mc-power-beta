package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.gameplay.GameplayConfig;
import net.minecraft.class_60;
import net.minecraft.class_142;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(class_60.class)
public class ExplosionItemsMixin {
  @Shadow private World field_1399;
  @Unique private int power$particles;
  @WrapOperation(method = "method_1195", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/Entity;I)Z"))
  private boolean power$protect(Entity entity, Entity source, int amount, Operation<Boolean> original) {
    if (!field_1399.isRemote && GameplayConfig.current().explosionProofItems && entity instanceof class_142) return false;
    return original.call(entity, source, amount);
  }
  @WrapOperation(method = "method_1195", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;sqrt(D)F"))
  private float power$finiteImpulse(double squared, Operation<Float> original) {
    float distance = original.call(squared);
    return distance == 0 && !field_1399.isRemote && (GameplayConfig.current().limitTnt || GameplayConfig.current().explosionProofItems)
        ? Float.MIN_NORMAL : distance;
  }
  @WrapOperation(method = "method_1196", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addParticle(Ljava/lang/String;DDDDDD)V"))
  private void power$particles(World world, String type, double x, double y, double z,
      double vx, double vy, double vz, Operation<Void> original) {
    if (!world.isRemote && GameplayConfig.current().limitTnt && power$particles++ >= 32) return;
    original.call(world, type, x, y, z, vx, vy, vz);
  }
}
