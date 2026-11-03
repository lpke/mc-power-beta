package local.luke.power.controls.mixin.tweaks.rawinput;

import local.luke.power.controls.tweaks.rawinput.RawInputHandler;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Mouse.class, remap = false)
public class MouseGrabMixin {
    @Inject(method = "setGrabbed", at = @At("HEAD"))
    private static void power$retireRawGrab(boolean grabbed, CallbackInfo ci) { RawInputHandler.beforeGrab(grabbed); }

    @Inject(method = "setGrabbed", at = @At("RETURN"))
    private static void power$publishRawGrab(boolean grabbed, CallbackInfo ci) { RawInputHandler.refreshCapture(); }
}
