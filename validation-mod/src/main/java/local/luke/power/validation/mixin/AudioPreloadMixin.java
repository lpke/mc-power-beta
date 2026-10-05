package local.luke.power.validation.mixin;

import local.luke.power.validation.AudioStartChecks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import paulscode.sound.Source;
import paulscode.sound.libraries.SourceLWJGLOpenAL;

@Mixin(value = SourceLWJGLOpenAL.class, remap = false)
public class AudioPreloadMixin {
  @Inject(method = "preLoad", at = @At("HEAD"))
  private void power$countPreload(CallbackInfoReturnable<Boolean> ci) {
    AudioStartChecks.preload(((Source) (Object) this).sourcename);
  }
}
