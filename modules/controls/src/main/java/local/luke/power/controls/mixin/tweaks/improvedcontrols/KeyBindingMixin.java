package local.luke.power.controls.mixin.tweaks.improvedcontrols;

import local.luke.power.controls.tweaks.controls.DefaultKeys;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyBinding.class)
public class KeyBindingMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(String translationKey, int code, CallbackInfo ci) {
        DefaultKeys.addDefaultKeybind(KeyBinding.class.cast(this), code);
    }
}
