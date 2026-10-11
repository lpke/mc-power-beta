package local.luke.power.commands.command.extra;

import java.util.Arrays;
import local.luke.power.commands.ClientCommands;
import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.SharedCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

public final class Perm implements Command {
  public String name() { return "perm"; }
  public boolean needsPermissions() { return false; }
  public boolean disableInSingleplayer() { return true; }
  public boolean available(SharedCommandSource source) {
    return source.isClient() && ((Minecraft)FabricLoader.getInstance().getGameInstance()).world.isRemote;
  }
  public void command(SharedCommandSource source, String[] args) {
    if (args.length != 1) { manual(source); return; }
    source.sendFeedback("Operator: " + ClientCommands.mp_op);
    source.sendFeedback("Server commands: " + ClientCommands.mp_rc);
    source.sendFeedback("Players: " + Arrays.toString(ClientCommands.player_names));
  }
  public void manual(SharedCommandSource source) { source.sendFeedback("/perm shows multiplayer command permissions and known players."); }
}
