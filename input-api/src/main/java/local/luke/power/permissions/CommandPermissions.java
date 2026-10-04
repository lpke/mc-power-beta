package local.luke.power.permissions;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import local.luke.power.storage.PowerConfig;

/** Access policy only. Never modifies a command's saved data or player inventory. */
public final class CommandPermissions {
  public enum Rule {
    ANY_MODE,
    CREATIVE_ONLY,
    DISABLED
  }

  public enum Override {
    INHERIT,
    ALLOW,
    BLOCK
  }

  public record Command(String name, String description, Rule defaultRule) {}

  public record Context(String world, boolean creative, boolean multiplayer) {}

  public static final List<Command> COMMANDS =
      List.of(
          command("clear", "Remove items from your inventory."),
          new Command(
              "gamemode",
              "Change survival, creative or spectator mode. Also controls /gm. The mode picker"
                  + " remains available.",
              Rule.ANY_MODE),
          command("give", "Add items to an inventory."),
          command(
              "god", "Toggle immunity to damage. Immunity is suspended while access is blocked."),
          command("hat", "Equip a held item as a hat."),
          command("heal", "Restore health."),
          command("kill", "Kill the selected player."),
          command("killall", "Remove entities from the world."),
          command("ride", "Mount a nearby entity."),
          command("summon", "Create an entity."),
          command("time", "Change the world time."),
          command("toggledownfall", "Change rain and storm conditions."),
          command("tp", "Teleport to coordinates or a player."),
          command(
              "warp",
              "Save, list or travel to named locations. Blocking access keeps all saved warps."));

  private static Command command(String name, String description) {
    return new Command(name, description, Rule.CREATIVE_ONLY);
  }

  public static final class Settings {
    public boolean enabled = true;
    public Map<String, Rule> rules = new LinkedHashMap<>();
    public Map<String, Map<String, Override>> worlds = new LinkedHashMap<>();

    public void validate() {
      if (rules == null || worlds == null || worlds.size() > 4096)
        throw new IllegalArgumentException("Invalid command permissions");
      if (rules.containsValue(null)) throw new IllegalArgumentException("Invalid command rule");
      worlds.forEach(
          (world, overrides) -> {
            if (world == null || overrides == null || overrides.containsValue(null))
              throw new IllegalArgumentException("Invalid world command permissions");
          });
    }

    public Rule rule(Command command) {
      return rules.getOrDefault(command.name, command.defaultRule);
    }

    public Override override(String world, String command) {
      return worlds.getOrDefault(world, Map.of()).getOrDefault(command, Override.INHERIT);
    }
  }

  private static final com.google.gson.Gson JSON = new com.google.gson.Gson();
  private static Settings current;
  private static Supplier<Context> context = () -> new Context("", false, false);

  public static void context(Supplier<Context> source) {
    context = Objects.requireNonNull(source);
  }

  public static Settings current() {
    if (current == null) {
      current = PowerConfig.read("commandAccess", Settings.class);
      current.validate();
    }
    return current;
  }

  public static Settings copy() {
    return JSON.fromJson(JSON.toJson(current()), Settings.class);
  }

  public static void preview(Settings value) {
    value.validate();
    current = JSON.fromJson(JSON.toJson(value), Settings.class);
  }

  public static void save(Settings value) throws IOException {
    value.validate();
    PowerConfig.save("commandAccess", value);
    preview(value);
  }

  public static String canonical(String name) {
    return name.equals("gm") ? "gamemode" : name;
  }

  public static boolean allowed(String command) {
    return denial(command).isEmpty();
  }

  public static String denial(String name) {
    String key = canonical(name);
    Command command = COMMANDS.stream().filter(c -> c.name.equals(key)).findFirst().orElse(null);
    if (command == null) return ""; // Help, information and WorldEdit keep their own access rules.
    try {
      return denial(current(), command, context.get());
    } catch (RuntimeException e) {
      return "Command access could not be checked. Reopen the world and check the game log.";
    }
  }

  public static String denial(Settings settings, Command command, Context context) {
    if (context.multiplayer) return "";
    if (!settings.enabled) return "Singleplayer commands are disabled in Options > Commands.";
    if (context.world.isEmpty()) return "Open a singleplayer world to use this command.";
    Override override = settings.override(context.world, command.name);
    if (override == Override.BLOCK) return "/" + command.name + " is disabled for this world.";
    if (override == Override.ALLOW) return "";
    return switch (settings.rule(command)) {
      case ANY_MODE -> "";
      case CREATIVE_ONLY ->
          context.creative
              ? ""
              : "/"
                  + command.name
                  + " requires creative mode. Change its access in Options > Commands.";
      case DISABLED -> "/" + command.name + " is disabled in Options > Commands.";
    };
  }
}
