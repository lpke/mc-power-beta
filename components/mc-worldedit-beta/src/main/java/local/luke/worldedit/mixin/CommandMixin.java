package local.luke.worldedit.mixin;

import local.luke.worldedit.WorldEditBeta;
import local.luke.worldedit.chat.ChatHistory;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Execute after chat has recorded the submitted line, before RetroCommands handles it again. */
@Mixin(value = ClientPlayerEntity.class, priority = 1200)
public class CommandMixin {
  @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
  private void worldedit$command(String text, CallbackInfo ci) {
    if (WorldEditBeta.command(
        (Minecraft) FabricLoader.getInstance().getGameInstance(), text.trim())) {
      ChatHistory.add(text.trim());
      ci.cancel();
    }
  }
}
