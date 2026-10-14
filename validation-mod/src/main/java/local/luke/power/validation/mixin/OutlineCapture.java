package local.luke.power.validation.mixin;

import local.luke.power.validation.Release172Checks;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class OutlineCapture {
  @Inject(method = "method_1545", at = @At("HEAD"))
  private void power$outlineDraw(CallbackInfo ci) {
    if (Release172Checks.captureOutline) Release172Checks.outlines++;
  }
}
