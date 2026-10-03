package local.luke.power.controls.mixin.tweaks.nopauseonlostfocus;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import local.luke.power.controls.ControlFeatures;
import local.luke.power.controls.tweaks.morekeybinds.KeyPressedListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public class class_555Mixin {
    @WrapWithCondition(method = "onFrameUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;pauseGame()V"))
    public boolean preventPauseScreen(Minecraft instance) {
        return ControlFeatures.GENERAL_CONFIG.pauseOnLostFocus && !KeyPressedListener.releasedMouse;
    }
}
