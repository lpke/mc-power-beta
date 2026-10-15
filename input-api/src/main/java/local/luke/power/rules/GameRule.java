package local.luke.power.rules;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.*;

/** The only catalog of saved world rules, command aliases and factory defaults. */
public enum GameRule {
  ALLOW_CREATIVE_MODE("allow_creative_mode", "Allow Creative mode", "allowCreativeMode", false, "Access", "Allow entering Creative. Returning to Survival is always available. Turning off returns an active Creative player safely to Survival."),
  ALLOW_DEATH_RECOVERY("allow_death_recovery", "Allow death recovery", "allowDeathRecovery", false, "Access", "Allow recovering recorded death items from the recovery screen. Viewing records remains available. Turning off preserves all records."),
  ALLOW_ENTERING_NETHER_USING_PORTALS("allow_entering_nether_using_portals", "Nether portal entry", "allowEnteringNetherUsingPortals", true, "World interactions", "Allow portal travel into the Nether. Return portals remain available. Commands use Allow teleportation."),
  ALLOW_ENTITY_CONTROL("allow_entity_control", "Allow entity control", "allowEntityControl", false, "Access", "Allow summoning, killing, removing and riding entities, including editing helpers that remove entities."),
  ALLOW_ITEM_SPAWNING("allow_item_spawning", "Allow item spawning", "allowItemSpawning", false, "Access", "Allow /give and item-spawning tools. Creative inventory actions remain available in Creative."),
  ALLOW_SPAWN_EDITING("allow_spawn_editing", "Allow spawn editing", "allowSpawnEditing", false, "Access", "Allow /spawnpoint and /setworldspawn. Sleeping can still set a personal respawn position."),
  ALLOW_SPECTATOR_MODE("allow_spectator_mode", "Allow Spectator mode", "allowSpectatorMode", false, "Access", "Allow entering Spectator. Turning off returns an active Spectator player safely to Survival. Freecam remains available."),
  ALLOW_SURVIVAL_ASSISTANCE("allow_survival_assistance", "Allow survival assistance", "allowSurvivalAssistance", false, "Access", "Allow /heal, /god, /clear and /repair. Repair restores the held tool or armour without changing its other data."),
  ALLOW_TELEPORTATION("allow_teleportation", "Allow teleportation", "allowTeleportation", false, "Access", "Allow teleport commands and travel to saved warps. Saving and listing warps remains available. Portal travel is separate."),
  ALLOW_TIME_EDITING("allow_time_editing", "Allow time editing", "allowTimeEditing", false, "Access", "Allow /time set, /time add and other persistent time edits. Time queries remain available."),
  ALLOW_WEATHER_EDITING("allow_weather_editing", "Allow weather editing", "allowWeatherEditing", false, "Access", "Allow /weather and /toggledownfall. Natural changes use Weather cycle."),
  ALLOW_WORLD_EDITING("allow_world_editing", "Allow world editing", "allowWorldEditing", false, "Access", "Allow block editing in any game mode. Selection, inspection and clipboard tools remain available. Entity removal and teleport helpers use their own rules."),
  ADVANCE_TIME("advance_time", "Daylight cycle", "doDaylightCycle", true, "Time and weather", "Advance the sun and daylight counter. When off, simulation and scheduled updates continue. Sleep and daylight displays use the same rule."),
  ADVANCE_WEATHER("advance_weather", "Weather cycle", "doWeatherCycle", true, "Time and weather", "Allow weather and its timers to advance naturally. When off, sleep must not clear frozen weather. Explicit weather editing uses its own permission."),
  CREEPER_GRIEFING("creeper_griefing", "Creeper block damage", "creeperGriefing", true, "World interactions", "Control blocks broken by creeper explosions. Entity damage remains unchanged."),
  DROWNING_DAMAGE("drowning_damage", "Drowning damage", "drowningDamage", true, "Player damage", "Allow player drowning damage. Does not globally remove oxygen handling or change damage to mobs."),
  FALL_DAMAGE("fall_damage", "Fall damage", "fallDamage", true, "Player damage", "Allow player fall damage. Optional modern water landings remain a separate mechanic. Does not control mob fall damage."),
  FIRE_DAMAGE("fire_damage", "Fire and lava damage", "fireDamage", true, "Player damage", "Allow player damage from fire and lava. Does not control fire spread, mob damage or dropped-item protection."),
  FIRE_SPREAD("fire_spread", "Fire spread", "doFireTick", true, "Time and weather", "Allow fire to spread, consume blocks and ignite from lava. Turning off preserves fire aging, extinguishing and damage."),
  GHAST_GRIEFING("ghast_griefing", "Ghast block damage", "ghastGriefing", true, "World interactions", "Control blocks broken by ghast explosions. Entity damage remains unchanged; ignition stays separate from block destruction."),
  KEEP_INVENTORY("keep_inventory", "Keep inventory on death", "keepInventory", false, "Drops and death", "Keep inventory, armour and player crafting inputs through death. Does not recover items from earlier deaths."),
  MOB_CROP_TRAMPLING("mob_crop_trampling", "Mob crop trampling", "mobCropTrampling", false, "World interactions", "Allow mobs to trample farmland. Player crop trampling is separate."),
  PLAYER_CROP_TRAMPLING("player_crop_trampling", "Player crop trampling", "playerCropTrampling", false, "World interactions", "Allow players to trample farmland, regardless of footwear. Mob crop trampling is separate."),
  LAVA_DESTROYS_ITEMS("lava_destroys_items", "Lava destroys dropped items", "lavaDestroysItems", true, "Drops and death", "Allow lava to burn dropped items. Fire, explosions, despawning and damage to players or mobs remain separate."),
  CACTUS_DESTROYS_ITEMS("cactus_destroys_items", "Cactus destroys dropped items", "cactusDestroysItems", true, "Drops and death", "Allow cactus contact to destroy dropped items. Does not change damage to players or mobs."),
  MOB_DROPS("mob_drops", "Mob loot", "doMobLoot", true, "Drops and death", "Allow mobs to drop loot, including extra loot from enabled mechanics. Player death inventories are separate."),
  PEACEFUL_HEALTH_REGENERATION("peaceful_health_regeneration", "Peaceful health regeneration", "peacefulHealthRegeneration", true, "Player damage", "Automatic Peaceful healing only. Food still heals directly."),
  PLAYERS_SLEEPING_PERCENTAGE("players_sleeping_percentage", "Players needed to skip night", "playersSleepingPercentage", 100, "World interactions", "Percentage of players needed to skip the night, for instant and accelerated sleep. Values above 100 prevent skipping. Frozen daylight and weather stay frozen."),
  SLEEP_AMBUSHES("sleep_ambushes", "Sleep ambushes", "sleepAmbushes", true, "Spawning", "Allow Beta sleep attacks when difficulty and Monster spawning permit them."),
  SPAWN_ANIMALS("spawn_animals", "Animal spawning", "spawnAnimals", true, "Spawning", "Allow natural animal spawning, including wolves and squid. Does not remove existing mobs or block /summon."),
  SPAWN_MONSTERS("spawn_monsters", "Monster spawning", "spawnMonsters", true, "Spawning", "Allow natural monster spawning. Peaceful still prevents monsters. Does not remove existing mobs or block /summon."),
  SPAWNER_BLOCKS_WORK("spawner_blocks_work", "Dungeon spawners", "spawnerBlocksEnabled", true, "Spawning", "Allow dungeon spawners to create mobs independently of natural spawning. Difficulty and local spawn conditions still apply."),
  TNT_EXPLODES("tnt_explodes", "TNT explosions", "tntExplodes", true, "World interactions", "Prevent new priming and pause existing primed TNT while off; preserve entities and fuses, then resume when enabled. Global TNT work limits remain independent."),
  EXPLOSIONS_DESTROY_ITEMS("explosions_destroy_items", "Explosions destroy dropped items", "explosionsDestroyItems", true, "Drops and death", "Allow explosions to damage and destroy dropped items. Applies to TNT, creepers and ghasts. Does not change block drops, player or mob damage, fire or lava.");

  private final String id, label, alias, group, description;
  private final JsonPrimitive defaultValue;

  GameRule(String id, String label, String alias, boolean value, String group, String description) {
    this(id, label, alias, new JsonPrimitive(value), group, description);
  }
  GameRule(String id, String label, String alias, int value, String group, String description) {
    this(id, label, alias, new JsonPrimitive(value), group, description);
  }
  GameRule(String id, String label, String alias, JsonPrimitive value, String group, String description) {
    this.id = id; this.label = label; this.alias = alias;
    this.defaultValue = value; this.group = group; this.description = description;
  }
  public String id() { return id; }
  public String label() { return label; }
  public String alias() { return alias; }
  public String group() { return group; }
  public String description() { return description; }
  public boolean access() { return group.equals("Access"); }
  public boolean integer() { return defaultValue.isNumber(); }
  public JsonPrimitive defaultValue() { return defaultValue; }
  public String help() { return description + "\n/gamerule " + id + "\nAlias: " + alias; }

  private static final Map<String, GameRule> NAMES = new HashMap<>();
  static {
    for (GameRule rule : values()) {
      if (NAMES.put(rule.id.toLowerCase(Locale.ROOT), rule) != null
          || NAMES.put(rule.alias.toLowerCase(Locale.ROOT), rule) != null)
        throw new ExceptionInInitializerError("Duplicate game-rule spelling: " + rule.id);
    }
  }
  public static GameRule find(String name) { return NAMES.get(name.toLowerCase(Locale.ROOT)); }
  public JsonPrimitive parse(String text) {
    if (!integer()) {
      if (text.equalsIgnoreCase("true")) return new JsonPrimitive(true);
      if (text.equalsIgnoreCase("false")) return new JsonPrimitive(false);
      throw new IllegalArgumentException(id + " needs true or false.");
    }
    try {
      if (!text.matches("[0-9]+")) throw new NumberFormatException();
      return checked(new JsonPrimitive(Integer.parseInt(text)));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(id + " needs a whole number from 0 to " + Integer.MAX_VALUE + ".");
    }
  }
  public JsonPrimitive checked(JsonElement value) {
    if (value == null || !value.isJsonPrimitive()) throw invalid();
    JsonPrimitive primitive = value.getAsJsonPrimitive();
    if (!integer()) { if (!primitive.isBoolean()) throw invalid(); return new JsonPrimitive(primitive.getAsBoolean()); }
    try {
      if (!primitive.isNumber()) throw invalid();
      int n = primitive.getAsBigDecimal().intValueExact();
      if (n < 0) throw invalid();
      return new JsonPrimitive(n);
    } catch (ArithmeticException | NumberFormatException e) { throw invalid(); }
  }
  private IllegalArgumentException invalid() {
    return new IllegalArgumentException("Invalid " + id + ": expected " + (integer() ? "a nonnegative whole number" : "true or false") + ".");
  }
}
