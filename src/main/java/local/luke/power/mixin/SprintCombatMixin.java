package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.sprint.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class SprintCombatMixin {
  @Inject(method = "damage", at = @At("RETURN"))
  private void power$combatDamage(Entity source, int damage, CallbackInfoReturnable<Boolean> cir) {
    if (source != null && cir.getReturnValue())
      SprintController.combat((PlayerEntity) (Object) this);
  }

  @WrapOperation(
      method = "attack",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/Entity;I)Z"))
  private boolean power$sprintHit(
      Entity target, Entity source, int damage, Operation<Boolean> original) {
    boolean sprint = SprintController.active(source);
    boolean hit = original.call(target, source, damage);
    if (hit) {
      SprintController.combat(source);
      if (sprint && SprintConfig.current().sprintKnockback) {
        double angle = source.yaw * Math.PI / 180;
        if (target instanceof LivingEntity) {
          target.velocityX = target.velocityX / 2 - Math.sin(angle) * .5;
          target.velocityZ = target.velocityZ / 2 + Math.cos(angle) * .5;
          if (target.field_1623) target.velocityY = Math.min(.4, target.velocityY / 2 + .5);
        } else {
          target.velocityX -= Math.sin(angle) * .5;
          target.velocityY += .1;
          target.velocityZ += Math.cos(angle) * .5;
        }
        source.velocityX *= .6;
        source.velocityZ *= .6;
        ((SprintPlayer) source).power$sprint().sprinting = false;
      }
    }
    return hit;
  }
}
