package local.luke.power.worldedit.mixin;

import local.luke.power.worldedit.WorldEditor;
import local.luke.power.worldedit.chat.ChatHistory;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

/** Own command execution outside all injected legacy dispatchers. Chat records history first. */
@Mixin(value = ClientPlayerEntity.class, priority = 1200)
public class CommandMixin {
  @WrapMethod(method = "sendChatMessage")
  private void power$command(String text, Operation<Void> original) {
    if (WorldEditor.command(
        (Minecraft) FabricLoader.getInstance().getGameInstance(), text.trim())) {
      ChatHistory.add(text.trim());
    } else original.call(text);
  }
}
