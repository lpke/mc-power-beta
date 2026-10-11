package local.luke.power.commands.mixin.intercept;

import local.luke.power.commands.ClientCommands;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.MultiplayerClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerClientPlayerEntity.class)
public class ClientPlayerMixin {
  @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
  public void sendFeedbackInMP(String par1, CallbackInfo ci) {
    if (par1.trim().equals("/clearchat")) {
      ((Minecraft) FabricLoader.getInstance().getGameInstance()).inGameHud.clearChat();
      ci.cancel();
    }
    String[] args = par1.trim().split("\\s+");
    var source = new local.luke.power.commands.util.SharedCommandSource((Object)this);
    if (local.luke.power.commands.util.CommandAccess.showHelp(source, par1)) {
      ci.cancel();
      return;
    }
    if (args[0].equalsIgnoreCase("/perm") || args[0].equalsIgnoreCase("/help")) {
      local.luke.power.commands.util.RetroChatUtil.handleCommand(source, par1.trim().substring(1), ClientCommands.mp_op);
      ci.cancel();
    }
  }
}
