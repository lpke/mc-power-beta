package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.chat.ChatColours;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(InGameHud.class)
public class ChatWrapMixin {
  @WrapOperation(method = "addChatMessage", at = @At(value = "INVOKE",
      target = "Ljava/lang/String;substring(I)Ljava/lang/String;"))
  private String power$continueColour(String message, int start, Operation<String> original) {
    return ChatColours.continuation(message, start) + original.call(message, start);
  }
}
