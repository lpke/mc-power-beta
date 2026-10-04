package local.luke.power.commands.util;

import java.util.Map;

/** Short descriptions for the command index; syntax lives with each command. */
public final class CommandHelp {
  private CommandHelp() {}

  private static final Map<String, String> TEXT =
      Map.ofEntries(
          Map.entry("help", "Command list; /help <command> shows syntax"),
          Map.entry("clear", "Remove inventory items; maxCount 0 only counts"),
          Map.entry("clearchat", "Clear chat history"),
          Map.entry("give", "Give items to a player"),
          Map.entry("gamemode", "Switch survival, creative or spectator mode"),
          Map.entry("tp", "Teleport entities; alias /teleport"),
          Map.entry("time", "Set, add or query world time"),
          Map.entry("weather", "Set clear, rain or thunder"),
          Map.entry("difficulty", "Read or change peaceful/easy/normal/hard"),
          Map.entry("seed", "Show the world seed"),
          Map.entry("spawnpoint", "Set a player's respawn position"),
          Map.entry("setworldspawn", "Set the world's default spawn"),
          Map.entry("summon", "Spawn a Beta entity"),
          Map.entry("kill", "Kill selected entities, or yourself"),
          Map.entry("ride", "Mount or dismount an entity"),
          Map.entry("warp", "Save, list or visit named positions"),
          Map.entry("god", "Toggle invulnerability"),
          Map.entry("heal", "Restore your health"),
          Map.entry("hat", "Swap your held item with your helmet"),
          Map.entry("killall", "Kill loaded non-player entities"),
          Map.entry("toggledownfall", "Toggle rain; /weather gives finer control"),
          Map.entry("mobs", "List Beta entity names"),
          Map.entry("id", "Look up an item ID"),
          Map.entry("clock", "Show the in-game time"),
          Map.entry("mods", "List installed components"),
          Map.entry("whoami", "Show your player name"));

  public static String description(String name) {
    return TEXT.getOrDefault(name, "");
  }
}
