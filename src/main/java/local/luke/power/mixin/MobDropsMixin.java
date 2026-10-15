package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.rules.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public class MobDropsMixin {
  @WrapOperation(method = "method_1327", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/World;method_210(Lnet/minecraft/entity/Entity;)Z"))
  private boolean power$loot(World world, Entity item, Operation<Boolean> original) {
    // Cover subclass death drops too, including skeleton-killed creeper records.
    if (!world.isRemote && (Object)this instanceof LivingEntity mob && mob.health <= 0
        && !(mob instanceof PlayerEntity) && !WorldRules.enabled(world.method_262(), GameRule.MOB_DROPS)) {
      item.markDead();
      return false;
    }
    return original.call(world, item);
  }
}
