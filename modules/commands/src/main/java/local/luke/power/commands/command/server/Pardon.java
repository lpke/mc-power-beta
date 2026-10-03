package local.luke.power.commands.command.server;

import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.ServerUtil;
import local.luke.power.commands.util.SharedCommandSource;

public class Pardon implements Command {
    @Override
    public void command(SharedCommandSource commandSource, String[] parameters) {
        if (parameters.length < 2) {
            manual(commandSource);
            return;
        }
        ServerUtil.getConnectionManager().unbanPlayer(parameters[1]);
        ServerUtil.sendFeedbackAndLog(commandSource.getName(), "Pardoning " + parameters[1]);
    }

    @Override
    public String name() {
        return "pardon";
    }

    @Override
    public void manual(SharedCommandSource commandSource) {
        commandSource.sendFeedback("Usage: /pardon {player}");
        commandSource.sendFeedback("Info: Pardons a banned player so that they can connect again");
    }

    @Override
    public boolean disableInSingleplayer() {
        return true;
    }
}
