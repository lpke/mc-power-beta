package local.luke.power.validation.mixin;

import local.luke.power.validation.ExtraSounds470Checks;
import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundManager.class)
public abstract class ExtraSound470Capture {
  @Inject(method = "method_2015", at = @At("HEAD"))
  private void validation$sound(String id, float x, float y, float z, float volume, float pitch, CallbackInfo ci) {
    ExtraSounds470Checks.capture(id, volume, pitch);
    local.luke.power.validation.ModernBow470Checks.captureSound(id,volume,pitch);
  }
}
