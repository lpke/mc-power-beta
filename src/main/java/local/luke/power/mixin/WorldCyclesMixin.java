package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.world.WorldCycles;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(World.class)
public abstract class WorldCyclesMixin {
  @Shadow protected WorldProperties properties;
  @Shadow public boolean isRemote;
  @Shadow protected int field_209;
  @Shadow protected float field_205, field_206, field_207, field_208;

  @Unique private WorldCycles power$cycles() { return (WorldCycles) properties; }

  @Redirect(method = "method_198", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldProperties;getTime()J"))
  private long power$skyTime(WorldProperties properties) {
    return isRemote ? properties.getTime() : power$cycles().power$daylightTime();
  }

  @ModifyVariable(method = "method_198", at = @At("HEAD"), argsOnly = true)
  private float power$skyInterpolation(float partialTick) {
    return !isRemote && power$cycles().power$daylightFrozen() ? 0 : partialTick;
  }

  @WrapOperation(method = "method_242", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldProperties;setTime(J)V", ordinal = 0))
  private void power$sleepTime(WorldProperties properties, long ignored, Operation<Void> original) {
    if (isRemote) { original.call(properties, ignored); return; }
    WorldCycles rules = power$cycles();
    if (!rules.power$daylightFrozen()) {
      long day = rules.power$daylightTime();
      long next = day + 24000L - Math.floorMod(day, 24000L);
      original.call(properties, properties.getTime() + next - day);
    }
  }

  @Inject(method = "method_245", at = @At("HEAD"), cancellable = true)
  private void power$weatherTick(CallbackInfo ci) {
    if (isRemote || !power$cycles().power$weatherFrozen()) return;
    // Flashes expire and rain fades normally, including after an explicit weather command.
    // The weather flags and their timers stay untouched.
    if (field_209 > 0) field_209--;
    field_205 = field_206;
    field_206 = Math.max(0, Math.min(1, field_206 + (properties.getRaining() ? .01f : -.01f)));
    field_207 = field_208;
    field_208 = Math.max(0, Math.min(1, field_208 + (properties.getThundering() ? .01f : -.01f)));
    ci.cancel();
  }

  @Inject(method = "method_273", at = @At("HEAD"), cancellable = true)
  private void power$sleepWeather(CallbackInfo ci) {
    if (!isRemote && power$cycles().power$weatherFrozen()) ci.cancel();
  }
}
