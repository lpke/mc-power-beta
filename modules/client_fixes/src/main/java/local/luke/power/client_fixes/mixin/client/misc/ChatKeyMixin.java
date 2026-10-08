package local.luke.power.client_fixes.mixin.client.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.GameOptions;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import local.luke.power.client_fixes.ClientFixesMod;
import local.luke.power.client_fixes.client.ClientFixesClientMod;
import local.luke.power.client_fixes.mixinterface.ChatScreenAccessor;

@Mixin(Minecraft.class)
public abstract class ChatKeyMixin {

    @Shadow public abstract boolean isWorldRemote();

    @Shadow public abstract void setScreen(Screen screen);

    @Shadow public GameOptions options;

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isWorldRemote()Z", ordinal = 0))
    private void onKey(CallbackInfo ci) {
        if (this.isWorldRemote() || ClientFixesMod.commandsAvailable) {
            if (Keyboard.getEventKey() == local.luke.power.input.Bindings.eventCode(this.options.chatKey)) {
                this.setScreen(((ChatScreenAccessor) new ChatScreen()).setInitialMessage(""));
            }
        }
    }
}
