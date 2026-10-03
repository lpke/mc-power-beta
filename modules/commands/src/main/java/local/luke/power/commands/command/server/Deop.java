package local.luke.power.commands.command.server;

import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.ServerUtil;
import local.luke.power.commands.util.SharedCommandSource;

public class Deop implements Command {
    @Override
    public void command(SharedCommandSource commandSource, String[] parameters) {
        if (parameters.length < 2) {
            manual(commandSource);
            return;
        }
        String playerName = parameters[1];
        ServerUtil.getConnectionManager().removeFromOperators(playerName);
        ServerUtil.sendFeedbackAndLog(commandSource.getName(), "De-opping " + playerName);
        ServerUtil.getConnectionManager().messagePlayer(playerName, "§eYou are no longer op!");

        if (ServerUtil.getConnectionManager().getPlayer(playerName) != null) {
            ServerUtil.informPlayerOpStatus(playerName);
        }
    }

    @Override
    public String name() {
        return "deop";
    }

    @Override
    public void manual(SharedCommandSource commandSource) {
        commandSource.sendFeedback("Usage: /deop {player}");
        commandSource.sendFeedback("Info: Removes op status from a player");
    }

    @Override
    public boolean disableInSingleplayer() {
        return true;
    }
}
