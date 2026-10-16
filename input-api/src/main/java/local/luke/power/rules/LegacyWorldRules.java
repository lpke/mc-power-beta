package local.luke.power.rules;

import com.google.gson.*;
import java.util.*;
import static local.luke.power.rules.GameRule.*;

/** The documented pre-rule world conversion. Never consults changing live preferences. */
public final class LegacyWorldRules {
  private LegacyWorldRules() {}

  public static RuleSet convert(JsonObject saved, JsonObject globals) {
    RuleSet rules = new RuleSet();
    boolean cheats = bool(saved, "cheats", false);
    String world = saved.has("world") ? saved.get("world").getAsString() : "";
    JsonObject access = object(globals, "commandAccess");
    boolean commands = cheats && bool(access, "enabled", true);
    Map<GameRule, List<String>> owners = Map.of(
        ALLOW_ITEM_SPAWNING, List.of("give"),
        ALLOW_ENTITY_CONTROL, List.of("summon", "kill", "killall", "ride"),
        ALLOW_SURVIVAL_ASSISTANCE, List.of("heal", "god", "clear"),
        ALLOW_TELEPORTATION, List.of("tp", "warp", "spawn"),
        ALLOW_SPAWN_EDITING, List.of("spawnpoint", "setworldspawn"),
        ALLOW_TIME_EDITING, List.of("time"),
        ALLOW_WEATHER_EDITING, List.of("toggledownfall"));
    for (var owner : owners.entrySet()) {
      Set<Boolean> values = new HashSet<>();
      for (String command : owner.getValue()) values.add(commands && command(access, world, command));
      if (values.size() != 1) throw conflict(owner.getKey(), "old command permissions disagree: " + String.join(", ", owner.getValue()));
      rules.set(owner.getKey(), values.iterator().next());
    }
    boolean modes = commands && command(access, world, "gamemode");
    for (var mode : Map.of("creative", ALLOW_CREATIVE_MODE, "spectator", ALLOW_SPECTATOR_MODE).entrySet()) {
      String override = string(object(object(access, "worldModeAccess"), world), mode.getKey(), "INHERIT");
      boolean allowed = switch (override) {
        case "ALLOW" -> true;
        case "BLOCK" -> false;
        case "INHERIT" -> bool(object(access, "modeAccess"), mode.getKey(), true);
        default -> throw conflict(mode.getValue(), "invalid old mode override");
      };
      rules.set(mode.getValue(), modes && allowed);
    }
    rules.set(ALLOW_WORLD_EDITING, cheats && bool(object(globals, "editor"), "enabled", true)
        && (!saved.has("editing") || saved.get("editing").getAsInt() != 2));
    rules.set(ALLOW_DEATH_RECOVERY, cheats);
    rules.set(KEEP_INVENTORY, cheats && bool(saved, "keepInventory", false));
    rules.set(ADVANCE_TIME, !cheats || bool(saved, "daylight", true));
    rules.set(ADVANCE_WEATHER, !cheats || bool(saved, "weather", true));
    JsonObject gameplay = object(globals, "gameplay");
    rules.set(FIRE_SPREAD, bool(gameplay, "fireSpread", true));
    rules.set(EXPLOSIONS_DESTROY_ITEMS, !bool(gameplay, "explosionProofItems", false));
    rules.set(SLEEP_NIGHTMARES, !bool(object(globals, "power_controls:tweaks"), "disableSleepSpawning", false));
    JsonObject mechanics = object(globals, "power_mechanics:config");
    JsonObject explosions = object(mechanics, "EXPLOSION_AND_FIRE_CONFIG");
    if (bool(explosions, "disableAllExplosionsBreakingBlocks", false) || bool(explosions, "disableTntExplosionBreakingBlocks", false))
      throw conflict(TNT_EXPLODES, "old block-only TNT protection has no equivalent. Choose whether TNT should explode before converting");
    rules.set(CREEPER_GRIEFING, !bool(explosions, "disableCreeperExplosionBreakingBlocks", false));
    rules.set(GHAST_GRIEFING, !bool(explosions, "disableGhastExplosionBreakingBlocks", false));
    boolean farmlandProtected = bool(object(mechanics, "MOB_CONFIG"), "disableTramplingFarmland", false);
    rules.set(MOB_CROP_TRAMPLING, !farmlandProtected);
    rules.set(PLAYER_CROP_TRAMPLING, !farmlandProtected);
    JsonObject environment = object(globals, "power_environment:config");
    JsonElement sleep = object(environment, "SLEEP_CONFIG").get("asleepPlayerPercentageForSkippingNight");
    if (sleep != null) {
      try {
        if (!sleep.isJsonPrimitive() || !sleep.getAsJsonPrimitive().isNumber()) throw new ArithmeticException();
        int percent = sleep.getAsBigDecimal().multiply(java.math.BigDecimal.valueOf(100)).intValueExact();
        if (percent > 100) { rules.set(ALLOW_SLEEP, false); percent = 100; }
        rules.set(PLAYERS_SLEEPING_PERCENTAGE, percent);
        if (percent < 100) rules.set(SLEEP_NIGHTMARES, false);
      } catch (RuntimeException e) { throw conflict(PLAYERS_SLEEPING_PERCENTAGE, "old sleeping fraction is not an exact non-negative whole percentage"); }
    }
    JsonObject species = object(object(environment, "ENTITY_SPAWN_MECHANICS_CONFIG"), "SPAWN_LIST_CONFIG");
    natural(rules, species, SPAWN_MONSTERS, !cheats || bool(saved, "hostile", true),
        "Creeper", "Ghast", "Skeleton", "Slime", "Spider", "Zombie", "ZombiePigman");
    natural(rules, species, SPAWN_ANIMALS, !cheats || bool(saved, "passive", true),
        "Chicken", "Cow", "Pig", "Sheep", "Squid", "Wolf");
    return rules;
  }
  private static void natural(RuleSet rules, JsonObject species, GameRule rule, boolean enabled, String... names) {
    Set<Boolean> values = new HashSet<>();
    for (String name : names) values.add(enabled && bool(species, "enable" + name + "EntitySpawning", true));
    if (values.size() != 1) throw conflict(rule, "old species switches disagree. Choose one value for this category before converting");
    rules.set(rule, values.iterator().next());
  }
  private static boolean command(JsonObject access, String world, String command) {
    String override = string(object(object(access, "worlds"), world), command, "INHERIT");
    return switch (override) {
      case "ALLOW" -> true;
      case "BLOCK" -> false;
      case "INHERIT" -> switch (string(object(access, "rules"), command, "ALLOWED")) {
        case "ALLOWED" -> true;
        case "DISABLED" -> false;
        default -> throw new IllegalArgumentException("Invalid old command permission: " + command);
      };
      default -> throw new IllegalArgumentException("Invalid old world command permission: " + command);
    };
  }
  private static JsonObject object(JsonObject parent, String key) {
    return parent.has(key) ? parent.getAsJsonObject(key) : new JsonObject();
  }
  private static String string(JsonObject parent, String key, String fallback) {
    if (!parent.has(key)) return fallback;
    JsonPrimitive value = parent.getAsJsonPrimitive(key);
    if (!value.isString()) throw new IllegalArgumentException("Invalid legacy value: " + key);
    return value.getAsString();
  }
  private static boolean bool(JsonObject parent, String key, boolean fallback) {
    if (!parent.has(key)) return fallback;
    JsonPrimitive value = parent.getAsJsonPrimitive(key);
    if (!value.isBoolean()) throw new IllegalArgumentException("Invalid legacy value: " + key);
    return value.getAsBoolean();
  }
  private static IllegalArgumentException conflict(GameRule rule, String detail) {
    return new IllegalArgumentException("Cannot convert " + rule.id() + ": " + detail + ". Original world is preserved.");
  }
}
