package local.luke.power.controls.mixin.tweaks.mainmenupanorama;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.controls.ControlFeatures;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {
    @Unique private boolean power$panoramaEnabled() {
        return ControlFeatures.USER_INTERFACE_CONFIG.panoramaConfig.enablePanorma
            && !local.luke.power.input.PanoramaCatalog.folder(ControlFeatures.USER_INTERFACE_CONFIG.panoramaConfig.panoramaFolder).isEmpty();
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/TitleScreen;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V", ordinal = 0))
    public void moveVersionText(TitleScreen instance, TextRenderer textRenderer, String text, int x, int y, int color, Operation<Void> original) {
        if (power$panoramaEnabled()) {
            textRenderer.drawWithShadow(text, x, this.height - 10, Color.white.getRGB());
        } else {
            original.call(instance, textRenderer, text, x, y, color);
        }
    }


    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/TitleScreen;renderBackground()V"))
    public void cancelDefaultBackgroundRendering(TitleScreen instance, Operation<Void> original) {
        // Redirect to nothing
        if (!power$panoramaEnabled()) {
            original.call(instance);
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/TitleScreen;renderBackground()V", shift = At.Shift.AFTER))
    public void redirectBackgroundRendering(int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (power$panoramaEnabled()) {
            GL11.glViewport(0, 0, this.minecraft.displayWidth, this.minecraft.displayHeight);
            drawPanorama(delta);
            fillGradient(0, 0, this.width, this.height, -2130706433, 16777215);
            fillGradient(0, 0, this.width, this.height, 0, Integer.MIN_VALUE);
        }
    }

    @Unique
    private void drawPanorama(float delta) {
        var settings = ControlFeatures.USER_INTERFACE_CONFIG.panoramaConfig;
        local.luke.power.input.PanoramaRenderer.render(minecraft.textureManager::getTextureId,
            settings.panoramaFolder, settings.blurBackground, local.luke.power.input.PanoramaRenderer.animationTicks(), minecraft.displayWidth, minecraft.displayHeight);
    }
}
