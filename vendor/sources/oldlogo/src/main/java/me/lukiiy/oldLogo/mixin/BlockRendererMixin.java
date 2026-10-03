package me.lukiiy.oldLogo.mixin;

import me.lukiiy.oldLogo.OldLogo;
import net.minecraft.class_13;
import net.minecraft.client.render.Tessellator;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(class_13.class)
public class BlockRendererMixin {
    @Redirect(method = "method_48", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Tessellator;normal(FFF)V"))
    private void oldLogo$faceLighting(Tessellator instance, float x, float y, float z) {
        if (!OldLogo.isLogoRendering) {
            instance.normal(x, y, z);
            return;
        }

        float mul = .6f;

        if (y < 0 || y > 0) mul = .5f;
        else if (z != 0) mul = .8f;

        GL11.glColor4f(mul, mul, mul, 1);
        instance.normal(x, y, z);
    }
}
