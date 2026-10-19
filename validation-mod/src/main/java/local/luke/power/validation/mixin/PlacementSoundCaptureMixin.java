package local.luke.power.validation.mixin;

import local.luke.power.validation.RedstoneSoundChecks;
import local.luke.power.validation.Release300ExtraSoundsChecks;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class PlacementSoundCaptureMixin {
  @Inject(method = "method_150", at = @At("HEAD"))
  private void validation$sound(double x, double y, double z, String id, float volume, float pitch, CallbackInfo ci) {
    RedstoneSoundChecks.capture(x, y, z, id, volume, pitch);
    Release300ExtraSoundsChecks.capture(x, y, z, id, volume, pitch);
  }

  @Inject(method = "method_191", at = @At("HEAD"))
  private void validation$entitySound(Entity entity, String id, float volume, float pitch, CallbackInfo ci) {
    Release300ExtraSoundsChecks.capture(entity.x, entity.y - entity.field_1631, entity.z, id, volume, pitch);
  }

  @Inject(method = "method_229", at = @At("HEAD"), cancellable = true)
  private void validation$reject(int x, int y, int z, int block, CallbackInfoReturnable<Boolean> ci) {
    if (RedstoneSoundChecks.reject && block == 55) ci.setReturnValue(false);
  }
}
