package local.luke.power.commands.api;

import local.luke.power.commands.util.RetroChatUtil;

public class CommandRegistry {
    public static void add(Command command) {
        RetroChatUtil.commands.add(command);
    }
}
