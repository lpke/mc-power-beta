package local.luke.power.validation.mixin;

import local.luke.power.validation.RedstoneSoundChecks;
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
  }

  @Inject(method = "method_229", at = @At("HEAD"), cancellable = true)
  private void validation$reject(int x, int y, int z, int block, CallbackInfoReturnable<Boolean> ci) {
    if (RedstoneSoundChecks.reject && block == 55) ci.setReturnValue(false);
  }
}
