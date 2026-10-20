package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.chat.*;
import local.luke.power.chat.ChatRetention;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(InGameHud.class)
public class ChatWrapMixin implements ChatNotes.History {
  public void power$removeSavedMessages() { power$retention.removeOwn(messages); }
  @Shadow private List<ChatHudLine> messages;
  @Unique private final ChatRetention<ChatHudLine> power$retention = new ChatRetention<>();
  @Unique private int power$depth, power$ownLimit, power$otherLimit;
  @Unique private long power$group;
  @WrapMethod(method = "addChatMessage")
  private void power$retain(String message, Operation<Void> original) {
    boolean first = power$depth == 0;
    if (first && !PlayerChat.own() && PlayerChat.echo(message)) return;
    if (first) power$group = power$retention.group();
    power$depth++;
    try { original.call(message); }
    finally {
      power$depth--;
      if (first) power$trim();
    }
  }
  @WrapOperation(method = "addChatMessage", at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V"))
  private void power$tag(List<ChatHudLine> list, int index, Object value, Operation<Void> original) {
    var line = (ChatHudLine)value;
    power$retention.add(line, power$group, PlayerChat.own());
    if (PlayerChat.restoring()) line.age = 201;
    original.call(list, index, value);
  }
  @Unique private void power$trim() {
    power$ownLimit = ChatNotes.ownLimit(); power$otherLimit = ChatNotes.otherLimit();
    power$retention.trim(messages, power$ownLimit, power$otherLimit);
  }
  @Inject(method = "tick", at = @At("TAIL"))
  private void power$limits(CallbackInfo ci) {
    if (power$ownLimit != ChatNotes.ownLimit() || power$otherLimit != ChatNotes.otherLimit()) power$trim();
  }
  @Inject(method = "clearChat", at = @At("TAIL"))
  private void power$clear(CallbackInfo ci) { power$retention.clear(); }

  @WrapOperation(method = "addChatMessage", at = @At(value = "INVOKE",
      target = "Ljava/lang/String;substring(I)Ljava/lang/String;"))
  private String power$continueColour(String message, int start, Operation<String> original) {
    return ChatColours.continuation(message, start) + original.call(message, start);
  }
}
