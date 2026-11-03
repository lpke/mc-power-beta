package local.luke.power.validation.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.validation.CursorLatency473Checks;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = Keyboard.class, remap = false, priority = 3000)
public abstract class CursorLatency473KeyboardCapture {
  @WrapMethod(method = "next")
  private static boolean next(Operation<Boolean> original) {
    boolean available = original.call();
    if (available && CursorLatency473Checks.active()) {
      long dispatch = System.nanoTime();
      CursorLatency473Checks.event(CursorLatency473Checks.KEYBOARD, dispatch, Keyboard.getEventNanoseconds(),
          Keyboard.getEventKey(), Keyboard.getEventKeyState(), 0, 0);
    }
    return available;
  }
}
