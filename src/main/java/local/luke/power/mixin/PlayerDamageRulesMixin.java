package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.rules.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public class PlayerDamageRulesMixin {
  @WrapOperation(method = "baseTick", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/entity/LivingEntity;damage(Lnet/minecraft/entity/Entity;I)Z", ordinal = 1))
  private boolean power$drowning(LivingEntity entity, Entity source, int amount, Operation<Boolean> original) {
    return allowed(entity, GameRule.DROWNING_DAMAGE) && original.call(entity, source, amount);
  }

  @WrapOperation(method = "method_1389", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/entity/LivingEntity;damage(Lnet/minecraft/entity/Entity;I)Z"))
  private boolean power$fall(LivingEntity entity, Entity source, int amount, Operation<Boolean> original) {
    return allowed(entity, GameRule.FALL_DAMAGE) && original.call(entity, source, amount);
  }

  private static boolean allowed(Entity entity, GameRule rule) {
    return entity.world.isRemote || !(entity instanceof PlayerEntity) || WorldRules.enabled(entity.world.method_262(), rule);
  }
}
