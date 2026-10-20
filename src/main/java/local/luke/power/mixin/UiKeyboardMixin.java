package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.input.KeyEventModifiers;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Screen.class)
public abstract class UiKeyboardMixin {
  @WrapMethod(method = "onKeyboardEvent")
  private void power$keyboardModifiers(Operation<Void> original) {
    KeyEventModifiers.begin();
    try { original.call(); }
    finally { KeyEventModifiers.end(); }
  }
}
