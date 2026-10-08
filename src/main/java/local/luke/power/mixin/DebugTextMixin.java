package local.luke.power.mixin;

import local.luke.power.ui.DebugOverlay;
import net.minecraft.client.font.TextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextRenderer.class)
public abstract class DebugTextMixin {
  @Inject(method = "drawWithShadow", at = @At("HEAD"), cancellable = true)
  private void power$debugText(String text, int x, int y, int color, CallbackInfo ci) {
    if (DebugOverlay.capturing) {
      DebugOverlay.add(text, x, y);
      ci.cancel();
    }
  }
}
