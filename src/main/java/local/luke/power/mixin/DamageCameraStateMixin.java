package local.luke.power.mixin;

import local.luke.power.visual.DamageCameraState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class DamageCameraStateMixin {
  @Inject(method = "damage", at = @At("RETURN"))
  private void power$damageSource(Entity attacker, int amount, CallbackInfoReturnable<Boolean> ci) {
    if (ci.getReturnValueZ()) ((DamageCameraState) this).power$recordHurt();
  }
}
