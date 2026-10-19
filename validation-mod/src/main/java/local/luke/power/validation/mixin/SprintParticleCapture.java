package local.luke.power.validation.mixin;

import local.luke.power.sprint.SprintParticle;
import local.luke.power.validation.Release260Checks;
import net.minecraft.class_75;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_75.class)
public class SprintParticleCapture {
  @Inject(method = "method_325", at = @At("HEAD"))
  private void power$capture(Particle particle, CallbackInfo ci) {
    if (Release260Checks.captureDust && particle instanceof SprintParticle dust)
      Release260Checks.dust.add(dust);
  }
}
