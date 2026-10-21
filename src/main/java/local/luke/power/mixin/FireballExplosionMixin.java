package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.world.ExplosionSource;
import net.minecraft.class_372;
import net.minecraft.class_60;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(class_372.class)
public class FireballExplosionMixin {
  @Shadow public LivingEntity field_2193;
  @WrapOperation(method = "tick", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/World;method_188(Lnet/minecraft/entity/Entity;DDDFZ)Lnet/minecraft/class_60;"))
  private class_60 power$origin(World world, Entity source, double x, double y, double z,
      float power, boolean fire, Operation<class_60> original) {
    return ExplosionSource.fireball(field_2193, () -> original.call(world, source, x, y, z, power, fire));
  }
}
