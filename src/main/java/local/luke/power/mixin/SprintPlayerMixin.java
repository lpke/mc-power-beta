package local.luke.power.mixin;

import local.luke.power.sprint.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class SprintPlayerMixin implements SprintPlayer {
  @Unique private final SprintState power$sprint = new SprintState();

  public SprintState power$sprint() {
    return power$sprint;
  }

  @Inject(method = "writeNbt", at = @At("TAIL"))
  private void power$saveStamina(NbtCompound nbt, CallbackInfo ci) {
    if (((PlayerEntity) (Object) this).world.isRemote) return;
    nbt.putDouble("PowerBetaStamina", power$sprint.stamina);
    nbt.putInt("PowerBetaCombatTicks", power$sprint.combatTicks);
    nbt.putInt("PowerBetaRechargeTicks", power$sprint.rechargeTicks);
    nbt.putBoolean("PowerBetaSprintExhausted", power$sprint.exhausted);
  }

  @Inject(method = "readtNbt", at = @At("TAIL"))
  private void power$readStamina(NbtCompound nbt, CallbackInfo ci) {
    if (!nbt.contains("PowerBetaStamina")) return;
    double value = nbt.getDouble("PowerBetaStamina");
    power$sprint.stamina = Double.isFinite(value) ? Math.max(0, Math.min(1000, value)) : 0;
    power$sprint.combatTicks = Math.max(0, Math.min(2400, nbt.getInt("PowerBetaCombatTicks")));
    power$sprint.rechargeTicks = Math.max(0, Math.min(1200, nbt.getInt("PowerBetaRechargeTicks")));
    power$sprint.exhausted = nbt.getBoolean("PowerBetaSprintExhausted");
  }
}
