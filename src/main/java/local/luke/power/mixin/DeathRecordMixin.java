package local.luke.power.mixin;

import local.luke.power.death.DeathTracker;
import local.luke.power.death.KeepInventory;
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
    local.luke.power.map.MapDeathCauses.capture((PlayerEntity)(Object)this,
        local.luke.power.death.DeathCause.describe((PlayerEntity)(Object)this, attacker));
    KeepInventory.begin((PlayerEntity)(Object)this);
    DeathTracker.died((PlayerEntity)(Object)this, attacker);
    var player = (PlayerEntity)(Object)this;
    if (!player.world.isRemote && !KeepInventory.kept(player)) {
      for (int i = 1; i <= 4; i++) {
        var slot = player.playerContainer.method_2084(i); var item = slot.getStack();
        if (item != null) { slot.setStack(null); player.method_509(item, true); }
      }
    }
  }
  @Inject(method = "tick", at = @At("HEAD"))
  private void power$newLife(CallbackInfo ci) {
    if (((PlayerEntity)(Object)this).health > 0) power$deathRecorded = false;
  }
}
