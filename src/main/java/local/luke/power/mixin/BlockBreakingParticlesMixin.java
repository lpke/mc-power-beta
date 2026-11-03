package local.luke.power.mixin;

import local.luke.power.input.BlockBreakingParticles;
import net.minecraft.class_75;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_75.class)
public class BlockBreakingParticlesMixin {
  /** Gate before native block reads, random sampling and particle allocation. */
  @Inject(method = {"method_321(IIII)V", "method_322(IIIII)V"}, at = @At("HEAD"), cancellable = true)
  private void power$blockParticles(CallbackInfo ci) {
    if (!BlockBreakingParticles.enabled()) ci.cancel();
  }
}
