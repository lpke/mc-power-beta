package local.luke.power.client_fixes.mixin.client.controls;

import lombok.Getter;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import local.luke.power.client_fixes.mixinterface.KeyBindingAccessor;

@Mixin(KeyBinding.class)
public class KeyBindingMixin implements KeyBindingAccessor {
    @Shadow
    public int code;

    @Unique
    @Getter
    private int defaultKeyCode;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(String translationKey, int code, CallbackInfo ci) {
        this.defaultKeyCode = this.code;
    }
}
