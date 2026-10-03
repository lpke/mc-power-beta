package local.luke.power.controls.mixin.tweaks.fpslimitslider;

import local.luke.power.controls.ControlFeatures;
import local.luke.power.controls.util.ModOptions;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;logGlError(Ljava/lang/String;)V", ordinal = 1))
    public void sync(CallbackInfo ci) {
        if (ModOptions.isFramerateLimited()) {
            Display.sync(ModOptions.getFpsLimitValue());
        }
    }
}
