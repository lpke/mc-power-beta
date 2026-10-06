package local.luke.power.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fix events and polling together, including function keys omitted by legacy LWJGL. */
@Pseudo
@Mixin(targets = "org.lwjgl.opengl.LinuxKeycodes", remap = false)
public abstract class LinuxKeycodesMixin {
  @Inject(method = "mapKeySymToLWJGLKeyCode", at = @At("HEAD"), cancellable = true)
  private static void power$extendedKeys(long keysym, CallbackInfoReturnable<Integer> cir) {
    int code = local.luke.power.input.ExtendedKeys.linux(keysym);
    if (code != 0) cir.setReturnValue(code);
  }
}
