package local.luke.power.commands.command.server;

import java.util.Arrays;
import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.*;
import net.minecraft.network.packet.play.ChatMessagePacket;

public final class Me implements Command {
  public String name() { return "me"; }
  public boolean disableInSingleplayer() { return true; }
  public boolean needsPermissions() { return false; }
  public void command(SharedCommandSource source, String[] args) {
    if (args.length < 2) { manual(source); return; }
    ServerUtil.getConnectionManager().sendToAll(new ChatMessagePacket("* " + source.getName() + " " + String.join(" ", Arrays.copyOfRange(args, 1, args.length))));
  }
  public void manual(SharedCommandSource source) { source.sendFeedback("/me <action> broadcasts an action to other players."); }
}
