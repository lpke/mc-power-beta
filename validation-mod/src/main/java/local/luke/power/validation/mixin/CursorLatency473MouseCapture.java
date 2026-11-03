package local.luke.power.validation.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.validation.CursorLatency473Checks;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Outer release timing includes production wrappers and LWJGL's global-lock acquisition. */
@Mixin(value = Mouse.class, remap = false, priority = 3000)
public abstract class CursorLatency473MouseCapture {
  @Shadow private static boolean isGrabbed;

  @WrapMethod(method = "setGrabbed")
  private static void release(boolean requested, Operation<Void> original) {
    int token = CursorLatency473Checks.beforeRelease(requested, isGrabbed);
    boolean completed = false;
    try { original.call(requested); completed = true; }
    finally { if (token != 0) CursorLatency473Checks.afterRelease(token, completed); }
  }

  @WrapMethod(method = "next")
  private static boolean next(Operation<Boolean> original) {
    boolean available = original.call();
    if (available && CursorLatency473Checks.active()) {
      long dispatch = System.nanoTime();
      CursorLatency473Checks.event(CursorLatency473Checks.MOUSE, dispatch, Mouse.getEventNanoseconds(),
          Mouse.getEventButton(), Mouse.getEventButtonState(), Mouse.getEventDX(), Mouse.getEventDY());
    }
    return available;
  }
}
