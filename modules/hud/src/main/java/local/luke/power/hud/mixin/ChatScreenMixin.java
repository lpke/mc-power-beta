package local.luke.power.hud.mixin;

import local.luke.power.hud.chat.OpenChatHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** UI drag scrolling continues while the world and native chat aging are paused. */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
    @Inject(method = "tick", at = @At("TAIL"))
    private void power$chatDrag(CallbackInfo ci) {
        if (minecraft.inGameHud instanceof OpenChatHud.Scrolling scroll) scroll.power$chatScroll(height);
    }
}
