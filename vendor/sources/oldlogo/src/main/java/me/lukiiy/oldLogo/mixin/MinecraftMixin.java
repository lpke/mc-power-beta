package me.lukiiy.oldLogo.mixin;

import me.lukiiy.oldLogo.OldLogo;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void oldLogo$get(CallbackInfo ci) {
        OldLogo.minecraft = (Minecraft) (Object) this;
    }
}
