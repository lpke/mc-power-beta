package me.lukiiy.oldLogo.mixin;

import me.lukiiy.oldLogo.OldLogo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Screen.class)
public class ScreenMixin {
    @Shadow protected Minecraft minecraft;

    @ModifyArgs(method = "renderBackgroundTexture(I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Tessellator;vertex(DDDDD)V"))
    private void oldLogo$scrollBackground(Args args) {
        if (!OldLogo.scrollingBackground.get() || (OldLogo.scrollingBackgroundRestricted.get() && !(minecraft.currentScreen instanceof TitleScreen))) return; // wow.

        args.set(4, (double) args.get(4) + ((MinecraftAccessor) minecraft).getTicks() * OldLogo.scrollingBackgroundSpeed.get() / 10);
    }
}
