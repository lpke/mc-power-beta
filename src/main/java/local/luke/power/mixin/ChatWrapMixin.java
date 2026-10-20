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
import java.util.*;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(InGameHud.class)
public class ChatWrapMixin implements ChatNotes.History, ChatTranscript.Source {
  public void power$removeSavedMessages() { power$retention.removeOwn(messages); power$pruneSources(); }
  @Shadow private List<ChatHudLine> messages;
  @Unique private final ChatRetention<ChatHudLine> power$retention = new ChatRetention<>();
  @Unique private int power$depth, power$ownLimit, power$otherLimit;
  @Unique private long power$group;
  @Unique private final Map<Long, ChatTranscript.Message> power$originals = new HashMap<>();
  @Unique private final Map<ChatHudLine, ChatTranscript.Message> power$fallback = new IdentityHashMap<>();
  @Unique private List<ChatTranscript.Message> power$snapshot;
  @Unique private int power$size;
  @Unique private ChatHudLine power$first, power$last;
  @Inject(method = "<init>", at = @At("RETURN"))
  private void power$source(CallbackInfo ci) { ChatTranscript.source(this); }
  public List<ChatTranscript.Message> messages() {
    ChatHudLine first = messages.isEmpty() ? null : messages.get(0), last = messages.isEmpty() ? null : messages.get(messages.size()-1);
    if (power$snapshot != null && power$size == messages.size() && first == power$first && last == power$last) return power$snapshot;
    var result = new ArrayList<ChatTranscript.Message>(); var groups = new HashSet<Long>();
    var retained = Collections.newSetFromMap(new IdentityHashMap<ChatHudLine, Boolean>());
    for (ChatHudLine line : messages) {
      Long group = power$retention.groupOf(line);
      if (group != null) {
        if (groups.add(group)) result.add(power$originals.computeIfAbsent(group, ignored -> new ChatTranscript.Message(line.text)));
      } else { retained.add(line); result.add(power$fallback.computeIfAbsent(line, ignored -> new ChatTranscript.Message(line.text))); }
    }
    power$originals.keySet().retainAll(groups); power$fallback.keySet().retainAll(retained);
    power$size=messages.size(); power$first=first; power$last=last;
    return power$snapshot=List.copyOf(result);
  }
  @WrapMethod(method = "addChatMessage")
  private void power$retain(String message, Operation<Void> original) {
    boolean first = power$depth == 0;
    if (first && !PlayerChat.own() && PlayerChat.echo(message)) return;
    if (first) {
      power$group = power$retention.group();
      power$originals.put(power$group, new ChatTranscript.Message(message)); power$snapshot = null;
    }
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
    power$pruneSources();
  }
  @Unique private void power$pruneSources() {
    var groups = new HashSet<Long>();
    var retained = Collections.newSetFromMap(new IdentityHashMap<ChatHudLine, Boolean>());
    for (ChatHudLine line : messages) { Long group=power$retention.groupOf(line);if(group!=null)groups.add(group);else retained.add(line); }
    power$originals.keySet().retainAll(groups);power$fallback.keySet().retainAll(retained);power$snapshot=null;
  }
  @Inject(method = "tick", at = @At("TAIL"))
  private void power$limits(CallbackInfo ci) {
    if (power$ownLimit != ChatNotes.ownLimit() || power$otherLimit != ChatNotes.otherLimit()) power$trim();
  }
  @Inject(method = "clearChat", at = @At("TAIL"))
  private void power$clear(CallbackInfo ci) {
    power$retention.clear(); power$originals.clear(); power$fallback.clear(); power$snapshot=null; ChatSelection.history.clear();
  }

  @WrapOperation(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/hud/InGameHud;messages:Ljava/util/List;"))
  private List<ChatHudLine> power$openChat(InGameHud hud, Operation<List<ChatHudLine>> original) {
    var mc = (net.minecraft.client.Minecraft)net.fabricmc.loader.api.FabricLoader.getInstance().getGameInstance();
    return mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen ? List.of() : original.call(hud);
  }

  @WrapOperation(method = "addChatMessage", at = @At(value = "INVOKE",
      target = "Ljava/lang/String;substring(I)Ljava/lang/String;"))
  private String power$continueColour(String message, int start, Operation<String> original) {
    return ChatColours.continuation(message, start) + original.call(message, start);
  }
}
