package local.luke.power.validation.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.validation.CursorLatency473Checks;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;

/** Inside the existing actual-swap observer, beneath production's deferred-swap gate. */
@Mixin(value = Display.class, remap = false, priority = 450)
public abstract class CursorLatency473DisplayCapture {
  @WrapMethod(method = "swapBuffers")
  private static void swap(Operation<Void> original) {
    int token = CursorLatency473Checks.beforeSwap();
    boolean completed = false;
    try { original.call(); completed = true; }
    finally { if (token != 0) CursorLatency473Checks.afterSwap(token, completed); }
  }
}
