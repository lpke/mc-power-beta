package local.luke.power.title.mixin;

import local.luke.power.title.*;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TitleScreen.class, priority = 56676)
public class TitleScreenMixin extends net.minecraft.client.gui.screen.Screen {
    @Unique private final TitleLogoRenderer power$titleRenderer = new TitleLogoRenderer();
    @Unique private boolean power$initialised, power$animate;
    @Unique private long power$selection;
    @Unique private boolean power$animation() {
        return power$animate && power$selection == local.luke.power.ui.TitlePreview.selectionRevision()
            && TitleFeatures.animation.get();
    }
    @Inject(method = "init", at = @At("TAIL"))
    private void power_title$prepare(CallbackInfo ci) {
        TitleLogos.prepare(minecraft);
        if (!power$initialised) {
            power$initialised = true;
            power$animate = !TitleLogos.textured();
            power$selection = local.luke.power.ui.TitlePreview.selectionRevision();
        }
    }
    @Redirect(method = "render(IIF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/TitleScreen;drawTexture(IIIIII)V"))
    private void power_title$cancelOriginal(TitleScreen instance, int x, int y, int u, int v, int w, int h) {
        if (w != 155 || h != 44) instance.drawTexture(x, y, u, v, w, h);
    }
    @Inject(method = "tick", at = @At("HEAD"))
    private void power_title$tick(CallbackInfo ci) {
        if (!TitleLogos.textured() && power$animation()) power$titleRenderer.tick();
    }
    @Inject(method = "render(IIF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/TitleScreen;renderBackground()V", shift = At.Shift.AFTER))
    private void power_title$render(int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (TitleLogos.textured()) { TitleLogos.render(TitleFeatures.minecraft, width); return; }
        power$titleRenderer.draw(0, 0, width, 120, width, height, TitleFeatures.logo.get(),
            TitleFeatures.blockMap.get(), TitleFeatures.logoRotation.get(), TitleFeatures.invertedBlocks.get(),
            TitleFeatures.shadow.get(), power$animation(), TitleFeatures.animationSpeed.get().floatValue(), delta);
    }
}
