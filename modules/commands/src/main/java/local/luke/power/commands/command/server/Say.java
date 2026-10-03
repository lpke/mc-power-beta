package local.luke.power.commands.command.server;

import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.ServerUtil;
import local.luke.power.commands.util.SharedCommandSource;
import net.minecraft.network.packet.play.ChatMessagePacket;

public class Say implements Command {
    @Override
    public void command(SharedCommandSource commandSource, String[] parameters) {
        if (parameters.length < 2) {
            manual(commandSource);
            return;
        }
        String joinedString = ServerUtil.appendEnd(1, parameters);
        ServerUtil.LOGGER.info("[" + commandSource.getName() + "] " + joinedString);
        ServerUtil.getConnectionManager().sendToAll(new ChatMessagePacket("§d[Server] " + joinedString));
    }

    @Override
    public String name() {
        return "say";
    }

    @Override
    public void manual(SharedCommandSource commandSource) {
        commandSource.sendFeedback("Usage: /say");
        commandSource.sendFeedback("Info: Broadcasts a message to all players");
    }

    @Override
    public boolean disableInSingleplayer() {
        return true;
    }
}
