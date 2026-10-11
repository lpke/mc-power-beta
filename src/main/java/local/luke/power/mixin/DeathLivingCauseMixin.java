package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.death.DeathCause;
import net.minecraft.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class DeathLivingCauseMixin {
  @WrapOperation(method = "method_1389", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;damage(Lnet/minecraft/entity/Entity;I)Z"))
  private boolean power$fall(LivingEntity victim, Entity attacker, int amount, Operation<Boolean> original) {
    return DeathCause.damage(victim, "Fall", () -> original.call(victim, attacker, amount));
  }
  @WrapOperation(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;damage(Lnet/minecraft/entity/Entity;I)Z", ordinal = 0))
  private boolean power$suffocate(LivingEntity victim, Entity attacker, int amount, Operation<Boolean> original) {
    return DeathCause.damage(victim, "Suffocation", () -> original.call(victim, attacker, amount));
  }
  @WrapOperation(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;damage(Lnet/minecraft/entity/Entity;I)Z", ordinal = 1))
  private boolean power$drown(LivingEntity victim, Entity attacker, int amount, Operation<Boolean> original) {
    return DeathCause.damage(victim, "Drowning", () -> original.call(victim, attacker, amount));
  }
  @WrapOperation(method = "method_1319", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;damage(Lnet/minecraft/entity/Entity;I)Z"))
  private boolean power$void(LivingEntity victim, Entity attacker, int amount, Operation<Boolean> original) {
    return DeathCause.damage(victim, "Void", () -> original.call(victim, attacker, amount));
  }
}
