package local.luke.power.commands.command.server;

import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.ServerUtil;
import local.luke.power.commands.util.SharedCommandSource;

public class BanIp implements Command {
    @Override
    public void command(SharedCommandSource commandSource, String[] parameters) {
        String ip = ServerUtil.appendEnd(1, parameters).trim();
        ServerUtil.getConnectionManager().banIp(ip);
        ServerUtil.sendFeedbackAndLog(commandSource.getName(), "Banning ip " + ip);
    }

    @Override
    public String name() {
        return "ban-ip";
    }

    @Override
    public void manual(SharedCommandSource commandSource) {
        commandSource.sendFeedback("Usage: /ban-ip {ip}");
        commandSource.sendFeedback("Info: bans an IP address from the server");
    }

    @Override
    public boolean disableInSingleplayer() {
        return true;
    }
}
