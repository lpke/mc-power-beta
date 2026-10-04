package local.luke.power.commands.command.vanilla;

import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.SharedCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

public final class ClearChat implements Command {
  public String name() {
    return "clearchat";
  }

  public boolean needsPermissions() {
    return false;
  }

  public void manual(SharedCommandSource s) {
    s.sendFeedback("/clearchat | Clear visible chat history.");
  }

  public void command(SharedCommandSource s, String[] args) {
    if (args.length != 1) {
      manual(s);
      return;
    }
    if (s.isClient())
      ((Minecraft) FabricLoader.getInstance().getGameInstance()).inGameHud.clearChat();
  }
}
