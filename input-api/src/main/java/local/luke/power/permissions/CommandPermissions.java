package local.luke.power.permissions;

import java.util.*;
import java.util.function.Supplier;
import local.luke.power.rules.*;
import static local.luke.power.rules.GameRule.*;

/** One permission owner per action. Read-only subcommands remain available. */
public final class CommandPermissions {
  public record Context(String world, boolean multiplayer, RuleSet rules) {}
  private static Supplier<Context> context = () -> new Context("", false, new RuleSet());
  private CommandPermissions() {}
  public static void context(Supplier<Context> source) { context = Objects.requireNonNull(source); }
  public static Context context() { return context.get(); }

  public static String canonical(String name) {
    String command = name.toLowerCase(Locale.ROOT).replaceFirst("^/+", "");
    return switch (command) {
      case "gm" -> "gamemode";
      case "teleport" -> "tp";
      case "weather" -> "toggledownfall";
      default -> command;
    };
  }
  public static GameRule owner(String name) {
    return switch (canonical(name)) {
      case "give" -> ALLOW_ITEM_SPAWNING;
      case "summon", "kill", "killall", "ride", "remove", "rem", "rement", "butcher" -> ALLOW_ENTITY_CONTROL;
      case "heal", "god", "clear", "repair" -> ALLOW_SURVIVAL_ASSISTANCE;
      case "tp", "spawn", "warp", "up", "ceil", "ascend", "descend", "unstuck", "thru", "jumpto" -> ALLOW_TELEPORTATION;
      case "spawnpoint", "setworldspawn" -> ALLOW_SPAWN_EDITING;
      case "time" -> ALLOW_TIME_EDITING;
      case "toggledownfall" -> ALLOW_WEATHER_EDITING;
      default -> null;
    };
  }
  public static boolean allowed(GameRule rule) {
    try { Context c = context(); return !c.multiplayer && !c.world.isEmpty() && c.rules.enabled(rule); }
    catch (RuntimeException e) { return false; }
  }
  public static boolean allowed(String name) { return denial(name).isEmpty(); }
  public static boolean available(String name) {
    return Set.of("time", "warp", "clear", "gamemode").contains(canonical(name)) || allowed(name);
  }
  public static String modeDenial(String mode) {
    if (mode.equals("survival")) return "";
    GameRule rule = switch (mode) {
      case "creative" -> ALLOW_CREATIVE_MODE;
      case "spectator" -> ALLOW_SPECTATOR_MODE;
      default -> null;
    };
    return rule == null ? "Unknown game mode." : denial(rule, context());
  }
  public static String denial(String name) { return denial(owner(name), context()); }
  public static String denial(String name, String[] args) {
    if (readOnly(name, args)) return "";
    return denial(name);
  }
  public static boolean readOnly(String name, String[] args) {
    if (args.length == 1 || args.length == 2 && args[1].equalsIgnoreCase("help")) {
      // Bare /clear, /time, /warp and similar commands can have active defaults.
      if (args.length == 2) return true;
    }
    return switch (canonical(name)) {
      case "time" -> args.length > 1 && Set.of("query", "get").contains(args[1].toLowerCase(Locale.ROOT));
      case "warp" -> args.length == 1 || args.length > 1 && Set.of("list", "set", "add", "remove", "delete").contains(args[1].toLowerCase(Locale.ROOT));
      case "clear" -> args.length == 4 && args[3].equals("0");
      default -> false;
    };
  }
  public static String denial(GameRule rule, Context context) {
    if (context.multiplayer || rule == null) return "";
    if (context.world.isEmpty()) return "Open a singleplayer world first.";
    return context.rules.enabled(rule) ? "" : "Enable " + rule.label() + " in Options > Game rules (" + rule.id() + ").";
  }
}
