package local.luke.power.commands.command.server;

import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.ServerUtil;
import local.luke.power.commands.util.SharedCommandSource;

public class Stop implements Command {
    @Override
    public void command(SharedCommandSource commandSource, String[] parameters) {
        ServerUtil.sendFeedbackAndLog(commandSource.getName(), "Stopping the server..");
        ServerUtil.getServer().stop();
    }

    @Override
    public String name() {
        return "stop";
    }

    @Override
    public void manual(SharedCommandSource commandSource) {
        commandSource.sendFeedback("Usage: /stop");
        commandSource.sendFeedback("Info: Gracefully stops the server");
    }

    @Override
    public boolean disableInSingleplayer() {
        return true;
    }
}
