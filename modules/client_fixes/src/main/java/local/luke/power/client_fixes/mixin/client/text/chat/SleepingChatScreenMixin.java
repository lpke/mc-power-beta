package local.luke.power.client_fixes.mixin.client.text.chat;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.SleepingChatScreen;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import local.luke.power.client_fixes.client.ClientFixesClientMod;

@Mixin(SleepingChatScreen.class)
public class SleepingChatScreenMixin extends ChatScreen {
    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;enableRepeatEvents(Z)V", remap = false))
    private void redirectEnableInput(boolean enable) {
        super.init();
    }

    @Redirect(method = "removed", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;enableRepeatEvents(Z)V", remap = false))
    private void redirectDisableInput(boolean enable) {
        super.removed();
    }

    @ModifyConstant(method = "keyPressed", constant = @Constant(intValue = 28 /* Keyboard.KEY_RETURN */))
    private int ignoreEnter(int def) {
        return -1;
    }

    @Inject(method = "keyPressed", at = @At("HEAD"))
    private void onKeyPressedHead(char character, int keyCode, CallbackInfo ci) {
        if (keyCode == Keyboard.KEY_RETURN) {
            ClientFixesClientMod.cancelSetScreenNull = true;
        }
    }
}
