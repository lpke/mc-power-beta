package local.luke.power.controls.mixin.tweaks.rawinput;

import local.luke.power.controls.tweaks.rawinput.RawInputHandler;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserve each accepted focus edge, even when native polling dispatches out/in in one batch. */
@Mixin(targets = "org.lwjgl.opengl.LinuxDisplay", remap = false)
public class LinuxDisplayMixin {
    @Shadow private boolean focused;

    @Inject(method = {"setFocused", "checkInput", "createWindow"}, at = @At(value = "FIELD", target = "Lorg/lwjgl/opengl/LinuxDisplay;focused:Z",
            opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void power$rawFocus(CallbackInfo ci) { RawInputHandler.focusChanged(focused); }

    @Inject(method = "destroyWindow", at = @At("HEAD"))
    private void power$retireRawWindow(CallbackInfo ci) { RawInputHandler.focusChanged(false); }
}
