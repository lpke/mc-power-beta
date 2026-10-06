package local.luke.power.mixin;

import local.luke.power.input.ExtendedKeys;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Keyboard.class, remap = false)
public abstract class KeyboardNamesMixin {
  @Inject(method = "next", at = @At("RETURN"))
  private static void power$release(CallbackInfoReturnable<Boolean> cir) {
    if (cir.getReturnValue()) local.luke.power.input.FullscreenKey.releaseEvent(
        Keyboard.getEventKey(), Keyboard.getEventKeyState());
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
