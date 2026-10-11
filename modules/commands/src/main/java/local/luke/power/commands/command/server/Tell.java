package local.luke.power.commands.command.server;

import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.ParameterSuggestUtil;
import local.luke.power.commands.util.ServerUtil;
import local.luke.power.commands.util.SharedCommandSource;
import net.minecraft.network.packet.play.ChatMessagePacket;

import java.util.Arrays;

public class Tell implements Command {
    @Override
    public void command(SharedCommandSource commandSource, String[] parameters) {
        if (parameters.length < 3) { manual(commandSource); return; }
        if (parameters.length >= 3) {
            String a = parameters[1];
            String b = "";
            for (int i = 2; i < parameters.length; i++) {
                b += (i == 2 ? "" : " ") + parameters[i];
            }
            String message = "§7" + commandSource.getName() + " whispers " + b;
            System.out.println(message + " to " + a);
            if (!ServerUtil.getConnectionManager().sendPacket(a, new ChatMessagePacket(message))) {
                commandSource.sendFeedback("§cThere's no player by that name online.");
            }
        }
    }

    @Override public boolean disableInSingleplayer() { return true; }
    @Override public boolean needsPermissions() { return false; }

    @Override
    public String name() {
        return "tell";
    }

    @Override
    public void manual(SharedCommandSource commandSource) {
        commandSource.sendFeedback("Usage: /tell {player} <message>");
        commandSource.sendFeedback("Whisper to another player");
    }

    @Override
    public String[] suggestion(SharedCommandSource source, int parameterNum, String currentInput, String totalInput) {
        return parameterNum == 1 ? ParameterSuggestUtil.suggestPlayerName(currentInput) : new String[0];
    }
}
