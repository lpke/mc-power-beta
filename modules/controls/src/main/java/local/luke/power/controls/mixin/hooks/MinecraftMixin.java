package local.luke.power.controls.mixin.hooks;

import local.luke.power.controls.tweaks.morekeybinds.KeyPressedListener;
import local.luke.power.controls.tweaks.rawinput.RawInputHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "tick", at = @At("RETURN"))
    public void tickEnd(CallbackInfo ci) {
        RawInputHandler.tick();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;getEventKey()I", ordinal = 2, remap = false))
    public void keyHook(CallbackInfo ci) {
        KeyPressedListener.keyPress();
    }
}
