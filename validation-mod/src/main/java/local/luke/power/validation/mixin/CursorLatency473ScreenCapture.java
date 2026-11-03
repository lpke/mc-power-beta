package local.luke.power.validation.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.validation.CursorLatency473Checks;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = Screen.class, priority = 3000)
public abstract class CursorLatency473ScreenCapture {
  @WrapMethod(method = "onMouseEvent")
  private void mouse(Operation<Void> original) {
    int token = CursorLatency473Checks.beforeHandler(CursorLatency473Checks.MOUSE);
    boolean completed = false;
    try { original.call(); completed = true; }
    finally { if (token != 0) CursorLatency473Checks.afterHandler(token, completed); }
  }

  @WrapMethod(method = "onKeyboardEvent")
  private void keyboard(Operation<Void> original) {
    int token = CursorLatency473Checks.beforeHandler(CursorLatency473Checks.KEYBOARD);
    boolean completed = false;
    try { original.call(); completed = true; }
    finally { if (token != 0) CursorLatency473Checks.afterHandler(token, completed); }
  }
}
