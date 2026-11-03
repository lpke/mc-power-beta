package local.luke.power.controls.mixin.tweaks.rawinput;

import local.luke.power.controls.tweaks.rawinput.RawInputHandler;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "onFrameUpdate", at = @At("HEAD"))
    private void power$rawOwnership(float delta, CallbackInfo ci) { RawInputHandler.update(); }
}
