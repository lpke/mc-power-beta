package local.luke.power.validation.mixin;

import local.luke.power.validation.Release240Checks;
import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundManager.class)
public abstract class UiClickCapture {
  @Inject(method = "method_2009", at = @At("HEAD"))
  private void power$count(String id,float volume,float pitch,CallbackInfo ci) {
    if (id.equals("random.click")) Release240Checks.clicks++;
  }
}
