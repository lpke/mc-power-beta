package local.luke.power.mixin;

import local.luke.power.input.ExtendedKeys;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Keyboard.class, remap = false)
public abstract class KeyboardNamesMixin {
  @org.spongepowered.asm.mixin.Unique private static final int[] power$modifiers = {29, 157, 42, 54, 56, 184};
  @Inject(method = "next", at = @At("RETURN"))
  private static void power$release(CallbackInfoReturnable<Boolean> cir) {
    if (cir.getReturnValue()) {
      local.luke.power.input.FullscreenKey.releaseEvent(Keyboard.getEventKey(), Keyboard.getEventKeyState());
      local.luke.power.input.KeyEventModifiers.event(Keyboard.getEventKey(), Keyboard.getEventKeyState());
    } else {
      int physical = 0;
      for (int i = 0; i < power$modifiers.length; i++) if (Keyboard.isKeyDown(power$modifiers[i])) physical |= 1 << i;
      local.luke.power.input.KeyEventModifiers.reconcile(physical);
    }
  }

  @Inject(method = "getKeyName", at = @At("HEAD"), cancellable = true)
  private static void power$name(int key, CallbackInfoReturnable<String> cir) {
    String name = ExtendedKeys.name(key);
    if (name != null) cir.setReturnValue(name);
  }
  @Inject(method = "getKeyIndex", at = @At("HEAD"), cancellable = true)
  private static void power$code(String name, CallbackInfoReturnable<Integer> cir) {
    int code = ExtendedKeys.code(name);
    if (code != 0) cir.setReturnValue(code);
  }
}
