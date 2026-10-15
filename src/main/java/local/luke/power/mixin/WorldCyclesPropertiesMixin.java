package local.luke.power.mixin;

import local.luke.power.rules.*;
import local.luke.power.world.WorldCycles;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.WorldProperties;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldProperties.class)
public abstract class WorldCyclesPropertiesMixin implements WorldCycles {
  @Shadow private long time;
  @Unique private long power$daylightOffset;
  public boolean power$daylightCycle() { return WorldRules.enabled(this, GameRule.ADVANCE_TIME); }
  public void power$daylightCycle(boolean enabled) { WorldRules.of(this).set(GameRule.ADVANCE_TIME, enabled); }
  public boolean power$weatherCycle() { return WorldRules.enabled(this, GameRule.ADVANCE_WEATHER); }
  public void power$weatherCycle(boolean enabled) { WorldRules.of(this).set(GameRule.ADVANCE_WEATHER, enabled); }
  public boolean power$daylightFrozen() { return !power$daylightCycle(); }
  public boolean power$weatherFrozen() { return !power$weatherCycle(); }
  public long power$daylightTime() { return time - power$daylightOffset; }
  public void power$daylightTime(long value) { power$daylightOffset = time - value; }

  @Inject(method = "setTime", at = @At("HEAD"))
  private void power$advanceSimulation(long next, CallbackInfo ci) {
    if (power$daylightFrozen()) power$daylightOffset += next - time;
  }

  @Inject(method = "<init>(Lnet/minecraft/nbt/NbtCompound;)V", at = @At("RETURN"))
  private void power$read(NbtCompound tag, CallbackInfo ci) {
    power$daylightOffset = tag.getLong("PowerBetaDaylightOffset");
  }

  @Inject(method = "<init>(Lnet/minecraft/world/WorldProperties;)V", at = @At("RETURN"))
  private void power$copy(WorldProperties other, CallbackInfo ci) {
    WorldCycles source = (WorldCycles) other;
    power$daylightOffset = time - source.power$daylightTime();
  }

  @Inject(method = "updateProperties", at = @At("RETURN"))
  private void power$write(NbtCompound tag, NbtCompound player, CallbackInfo ci) {
    tag.putLong("PowerBetaDaylightOffset", power$daylightOffset);
  }
}
