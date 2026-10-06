package local.luke.power.mixin;

import local.luke.power.input.FullscreenKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "org.lwjgl.opengl.LinuxKeyboard", remap = false)
public abstract class LinuxKeyboardMixin {
  @Shadow private int getKeycode(long event, int state) { throw new AssertionError(); }

  @Inject(method = "handleKeyEvent", at = @At("HEAD"))
  private void power$releaseAfterRecreation(long event, long millis, int type, int nativeCode,
      int state, CallbackInfo ci) {
    // After fullscreen recreates the keyboard, LWJGL discards releases for keys it
    // no longer knows were held. Observe the real X KeyRelease before that filter.
    if (type == 3) FullscreenKey.releaseEvent(getKeycode(event, state), false);
  }
}
