package local.luke.power.mixin;

import local.luke.power.death.DeathTracker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class DeathRecordMixin {
  @Unique private boolean power$deathRecorded;
  @Inject(method = "method_938", at = @At("HEAD"))
  private void power$recordDeath(Entity attacker, CallbackInfo ci) {
    if (power$deathRecorded) return;
    power$deathRecorded = true;
    DeathTracker.died((PlayerEntity)(Object)this, attacker);
  }
  @Inject(method = "tick", at = @At("HEAD"))
  private void power$newLife(CallbackInfo ci) {
    if (((PlayerEntity)(Object)this).health > 0) power$deathRecorded = false;
  }
}
