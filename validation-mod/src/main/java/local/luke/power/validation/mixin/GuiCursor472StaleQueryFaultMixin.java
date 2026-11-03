package local.luke.power.validation.mixin;

import java.nio.IntBuffer;
import local.luke.power.validation.GuiCursor472StaleQueryFault;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Disabled unless the explicit cursor fixture arms its one-shot native-query fault. */
@Mixin(targets = "org.lwjgl.opengl.LinuxMouse", remap = false)
public abstract class GuiCursor472StaleQueryFaultMixin {
  @Shadow private static long nQueryPointer(long display, long window, IntBuffer result) {
    throw new AssertionError();
  }

  @Redirect(method = "reset", at = @At(value = "INVOKE",
      target = "Lorg/lwjgl/opengl/LinuxMouse;nQueryPointer(JJLjava/nio/IntBuffer;)J"))
  private long power$staleQueryFault(long display, long window, IntBuffer result, boolean grab, boolean warp) {
    long root = nQueryPointer(display, window, result);
    GuiCursor472StaleQueryFault.queried(grab, result);
    return root;
  }
}
