package local.luke.power.permissions;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import local.luke.power.storage.PowerConfig;

/** Access policy only. Never modifies a command's saved data or player inventory. */
public final class CommandPermissions {
  public enum Rule {
    ALLOWED,
    DISABLED
  }

  public enum Override {
    INHERIT,
    ALLOW,
    BLOCK
  }

  public record Command(String name, String description, Rule defaultRule, boolean cheat) {
    public Command(String name, String description, Rule defaultRule) {
      this(name, description, defaultRule, true);
    }
  }

  public record Context(String world, boolean multiplayer, boolean cheats) {}

  public static final List<Command> COMMANDS =
      List.of(
          command("clear", "Remove items from your inventory."),
          new Command(
              "gamemode",
              "Allow mode changes through /gamemode, /gm and the mode switcher. Returning to"
                  + " survival remains available.",
              Rule.ALLOWED),
          command("difficulty", "Change the game difficulty."),
          command(
              "spawnpoint", "Set a player respawn position without changing inventory or warps."),
          command(
              "setworldspawn", "Set the default world spawn for players without a personal spawn."),
          command("give", "Add items to an inventory."),
          command(
              "god", "Toggle immunity to damage. Immunity is suspended while access is blocked."),
          command("hat", "Equip a held item as a hat."),
          command("heal", "Restore health."),
          command("kill", "Kill selected entities or yourself."),
          command("killall", "Remove entities from the world."),
          command("ride", "Mount a nearby entity."),
          command("summon", "Create an entity."),
          command("time", "Change the world time."),
          command("toggledownfall", "Change rain and storm conditions. Also controls /weather."),
          command(
              "tp", "Teleport entities to coordinates or another entity. Also controls /teleport."),
          command(
              "warp",
              "Save, list or travel to named locations. Blocking access keeps all saved warps."),
          info("help", "Show command instructions."),
          info("seed", "Display the world seed."),
          info("clearchat", "Clear the visible chat history."),
          info("clock", "Display the time of day."),
          info("id", "Look up item IDs."),
          info("mobs", "List entity types."),
          info("mods", "List installed components."),
          info("whoami", "Display your player name."));

  private static Command command(String name, String description) {
    return new Command(name, description, Rule.ALLOWED);
  }

  private static Command info(String name, String description) {
    return new Command(name, description, Rule.ALLOWED, false);
  }

  public static final class Settings {
    public boolean enabled = true;
    public Map<String, Rule> rules = new LinkedHashMap<>();
    public Map<String, Map<String, Override>> worlds = new LinkedHashMap<>();
    public Map<String, Boolean> modeAccess = new LinkedHashMap<>();
    public Map<String, Map<String, Override>> worldModeAccess = new LinkedHashMap<>();

    public void validate() {
      if (rules == null || worlds == null || worlds.size() > 4096)
        throw new IllegalArgumentException("Invalid command permissions");
      if (modeAccess == null
          || worldModeAccess == null
          || modeAccess.values().stream().anyMatch(Objects::isNull)
          || worldModeAccess.size() > 4096)
        throw new IllegalArgumentException("Invalid mode permissions");
      worldModeAccess.forEach(
          (world, values) -> {
            if (world == null
                || values == null
                || values.values().stream().anyMatch(Objects::isNull))
              throw new IllegalArgumentException("Invalid world mode permissions");
          });
      if (rules.values().stream().anyMatch(Objects::isNull))
        throw new IllegalArgumentException("Invalid command rule");
      worlds.forEach(
          (world, overrides) -> {
            if (world == null
                || overrides == null
                || overrides.values().stream().anyMatch(Objects::isNull))
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
    return switch (name) {
      case "gm" -> "gamemode";
      case "teleport" -> "tp";
      case "weather" -> "toggledownfall";
      default -> name;
    };
  }

  public static String modeDenial(String mode) {
    if (mode.equals("survival")) return "";
    try {
      return modeDenial(current(), mode, context.get());
    } catch (RuntimeException e) {
      return "Mode access could not be checked. Reopen the world.";
    }
  }

  public static String modeDenial(Settings settings, String mode, Context context) {
    if (mode.equals("survival") || context.multiplayer) return "";
    if (!Set.of("creative", "spectator").contains(mode)) return "Unknown game mode.";
    Command command =
        COMMANDS.stream().filter(c -> c.name.equals("gamemode")).findFirst().orElseThrow();
    String gate = denial(settings, command, context);
    if (!gate.isEmpty()) return gate;
    Override world =
        settings
            .worldModeAccess
            .getOrDefault(context.world, Map.of())
            .getOrDefault(mode, Override.INHERIT);
    boolean allowed =
        world == Override.ALLOW
            || world == Override.INHERIT && settings.modeAccess.getOrDefault(mode, true);
    return allowed ? "" : "Entering " + mode + " is disabled in Options > Commands.";
  }

  public static boolean cheatsEnabled() {
    try {
      Context c = context.get();
      return !c.multiplayer && !c.world.isEmpty() && c.cheats;
    } catch (RuntimeException e) {
      return false;
    }
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
    if (command.cheat && !context.cheats)
      return "Cheats are disabled for this world. Enable Cheats in Options > General.";
    if (command.cheat && !settings.enabled)
      return "Cheat commands are disabled in Options > Commands.";
    if (context.world.isEmpty()) return "Open a singleplayer world to use this command.";
    Override override = settings.override(context.world, command.name);
    if (override == Override.BLOCK) return "/" + command.name + " is disabled for this world.";
    if (override == Override.ALLOW) return "";
    return switch (settings.rule(command)) {
      case ALLOWED -> "";
      case DISABLED -> "/" + command.name + " is disabled in Options > Commands.";
    };
  }
}
