package local.luke.power.mixin;

import local.luke.power.input.PlayerMode;
import local.luke.power.visual.VisualConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class SpectatorOutlineMixin {
  @Shadow private Minecraft field_1814;

  @Inject(method = "method_1554", at = @At("HEAD"), cancellable = true)
  private void power$spectatorOutline(CallbackInfo ci) {
    if (!VisualConfig.current().spectatorBlockOutlines
        && field_1814.player instanceof PlayerMode mode && mode.power$spectator()) ci.cancel();
  }
}
