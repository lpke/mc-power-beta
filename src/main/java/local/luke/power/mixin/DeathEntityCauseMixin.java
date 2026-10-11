package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.death.DeathCause;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import local.luke.power.world.DeathSources;

@Mixin(Entity.class)
public abstract class DeathEntityCauseMixin {
  @WrapMethod(method = "method_1383")
  private void power$lightning(net.minecraft.class_411 lightning, Operation<Void> original) {
    if ((Object)this instanceof net.minecraft.entity.player.PlayerEntity && local.luke.power.death.DeathConfig.current().enabled)
      DeathSources.run(this, "Lightning", () -> original.call(lightning));
    else original.call(lightning);
  }
  @WrapOperation(method = {"baseTick", "method_1392", "method_1332"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/Entity;I)Z"))
  private boolean power$fire(Entity victim, Entity attacker, int amount, Operation<Boolean> original) {
    if (!(victim instanceof net.minecraft.entity.player.PlayerEntity) || !local.luke.power.death.DeathConfig.current().enabled)
      return original.call(victim, attacker, amount);
    String cause = victim.world.method_169(victim.boundingBox, net.minecraft.block.Material.LAVA) ? "Lava" : "Fire";
    return DeathCause.damage(victim, cause, () -> original.call(victim, attacker, amount));
  }
}
