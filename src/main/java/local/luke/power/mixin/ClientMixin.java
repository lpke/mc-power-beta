package local.luke.power.mixin;

import local.luke.power.PowerBeta;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class ClientMixin {
  @Inject(method = "method_2139", at = @At("HEAD"))
  private void power$dimensionMusic(CallbackInfo ci) {
    local.luke.power.audio.AudioController.changingDimension();
  }

  @Inject(method = "tick", at = @At("HEAD"))
  private void power$bindings(CallbackInfo ci) {
    Minecraft mc = (Minecraft)(Object)this;
    local.luke.power.input.KeyConfig.load();
    if (mc.options != null) local.luke.power.input.Bindings.register(mc.options.allKeys);
  }
  @Inject(method = "tick", at = @At("TAIL"))
  private void power$tick(CallbackInfo ci) {
    PowerBeta.tick((Minecraft) (Object) this);
  }
}
