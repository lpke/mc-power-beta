package local.luke.tweaks.mixin;

import java.util.Arrays;
import local.luke.tweaks.camera.FreeLook;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameOptions.class)
public class CameraOptionsMixin {
  @Shadow public KeyBinding[] allKeys;

  @Inject(method = "getKeybindName", at = @At("HEAD"), cancellable = true)
  private void lpke$name(int index, CallbackInfoReturnable<String> cir) {
    if (index >= 0 && index < allKeys.length && allKeys[index] == FreeLook.KEY)
      cir.setReturnValue("Free Look");
  }

  @Inject(method = "load", at = @At("HEAD"))
  private void lpke$keys(CallbackInfo ci) {
    java.util.List<KeyBinding> bindings = new java.util.ArrayList<>(Arrays.asList(allKeys));
    if (!bindings.contains(FreeLook.KEY)) bindings.add(FreeLook.KEY);
    for (KeyBinding key : local.luke.tweaks.hotbar.Hotbars.ALL)
      if (!bindings.contains(key)) bindings.add(key);
    allKeys = bindings.toArray(KeyBinding[]::new);
  }
}
