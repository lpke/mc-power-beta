package local.luke.power.commands.api;

import local.luke.power.commands.util.SharedCommandSource;

public interface Command {
    void command(SharedCommandSource commandSource, String[] parameters);

    String name();

    default boolean available(SharedCommandSource source) { return true; }

    /** Commands sharing a help index can occupy one entry in /help. */
    default String helpGroup() { return ""; }

    default String[] suggestion(SharedCommandSource source, int parameterNum, String currentInput, String totalInput)
    {
        return new String[0];
    }

    void manual(SharedCommandSource commandSource);

    default boolean disableInSingleplayer()
    {
        return false;
    }

    default boolean
    needsPermissions() {
        return true;
    }
}
