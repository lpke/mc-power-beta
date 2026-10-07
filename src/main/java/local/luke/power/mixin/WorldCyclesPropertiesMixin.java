package local.luke.power.mixin;

import local.luke.power.permissions.CheatWorld;
import local.luke.power.world.WorldCycles;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.WorldProperties;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldProperties.class)
public abstract class WorldCyclesPropertiesMixin implements WorldCycles {
  @Shadow private long time;
  @Unique private boolean power$daylight = true, power$weather = true;
  @Unique private long power$daylightOffset;

  public boolean power$daylightCycle() { return power$daylight; }
  public void power$daylightCycle(boolean enabled) { power$daylight = enabled; }
  public boolean power$weatherCycle() { return power$weather; }
  public void power$weatherCycle(boolean enabled) { power$weather = enabled; }
  public boolean power$daylightFrozen() { return !power$daylight && ((CheatWorld) this).power$cheatsEnabled(); }
  public boolean power$weatherFrozen() { return !power$weather && ((CheatWorld) this).power$cheatsEnabled(); }
  public long power$daylightTime() { return time - power$daylightOffset; }
  public void power$daylightTime(long value) { power$daylightOffset = time - value; }

  @Inject(method = "setTime", at = @At("HEAD"))
  private void power$advanceSimulation(long next, CallbackInfo ci) {
    if (power$daylightFrozen()) power$daylightOffset += next - time;
  }

  @Inject(method = "<init>(Lnet/minecraft/nbt/NbtCompound;)V", at = @At("RETURN"))
  private void power$read(NbtCompound tag, CallbackInfo ci) {
    power$daylight = !tag.contains("PowerBetaDaylightCycle") || tag.getBoolean("PowerBetaDaylightCycle");
    power$weather = !tag.contains("PowerBetaWeatherCycle") || tag.getBoolean("PowerBetaWeatherCycle");
    power$daylightOffset = tag.getLong("PowerBetaDaylightOffset");
  }

  @Inject(method = "<init>(Lnet/minecraft/world/WorldProperties;)V", at = @At("RETURN"))
  private void power$copy(WorldProperties other, CallbackInfo ci) {
    WorldCycles source = (WorldCycles) other;
    power$daylight = source.power$daylightCycle();
    power$weather = source.power$weatherCycle();
    power$daylightOffset = time - source.power$daylightTime();
  }

  @Inject(method = "updateProperties", at = @At("RETURN"))
  private void power$write(NbtCompound tag, NbtCompound player, CallbackInfo ci) {
    tag.putBoolean("PowerBetaDaylightCycle", power$daylight);
    tag.putBoolean("PowerBetaWeatherCycle", power$weather);
    tag.putLong("PowerBetaDaylightOffset", power$daylightOffset);
  }
}
