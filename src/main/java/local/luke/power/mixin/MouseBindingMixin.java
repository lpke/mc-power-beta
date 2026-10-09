package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.input.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public class MouseBindingMixin {
  @Unique private long power$mouseEvent = Long.MIN_VALUE;
  @Unique private boolean power$mouseConsumed;

  @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventButton()I", remap = false))
  private int power$mousePress(Operation<Integer> original) {
    int button = original.call();
    long event = Mouse.getEventNanoseconds();
    if (event != power$mouseEvent) {
      power$mouseEvent = event;
      power$mouseConsumed = false;
      Minecraft mc = (Minecraft)(Object)this;
      if (button >= 0 && Mouse.getEventButtonState() && mc.field_2778 && mc.currentScreen == null
          && mc.player != null && Bindings.claimsMouse(button - 100)) {
        power$mouseConsumed = true;
        MouseActions.press(mc, button - 100);
      }
    }
    return power$mouseConsumed ? -1 : button;
  }

  @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;isButtonDown(I)Z", remap = false))
  private boolean power$heldMouse(int button, Operation<Boolean> original) {
    return button == 0 && AttackInput.held()
        || !Bindings.claimsMouse(button - 100) && original.call(button);
  }
}
