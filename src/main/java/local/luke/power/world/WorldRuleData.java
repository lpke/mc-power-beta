package local.luke.power.world;

import com.google.gson.*;
import local.luke.power.rules.*;
import local.luke.power.storage.PowerConfig;
import net.minecraft.nbt.*;

/** Native NBT boundary. Validation runs before any converted values are published. */
public final class WorldRuleData {
  public static final String TAG = "GameRules";
  private WorldRuleData() {}

  public static RuleSet read(NbtCompound world) {
    if (!world.contains(TAG)) return legacy(world);
    requireType(world, TAG, 10);
    NbtCompound tag = world.getCompound(TAG);
    requireType(tag, "version", 3);
    int version = tag.getInt("version");
    if (version != RuleSet.VERSION)
      throw new IllegalArgumentException("World game rules version " + version + " is unsupported. Supported version: " + RuleSet.VERSION + ". Open with a compatible release or restore a backup.");
    JsonObject values = new JsonObject();
    for (Object value : tag.values()) {
      NbtElement entry = (NbtElement) value;
      String id = entry.getKey();
      if (id.equals("version")) continue;
      GameRule rule = GameRule.find(id);
      if (rule == null || !rule.id().equals(id)) throw new IllegalArgumentException("Unknown saved game rule: " + id + ". Open with a compatible release.");
      requireType(tag, id, rule.integer() ? 3 : 1);
      if (rule.integer()) values.addProperty(id, tag.getInt(id));
      else {
        byte number = tag.getByte(id);
        if (number != 0 && number != 1) throw new IllegalArgumentException("Invalid boolean game rule: " + id);
        values.addProperty(id, number == 1);
      }
    }
    return RuleSet.readValues(values, new RuleSet());
  }
  public static void write(NbtCompound world, RuleSet rules) {
    NbtCompound tag = new NbtCompound(); tag.putInt("version", RuleSet.VERSION);
    for (GameRule rule : GameRule.values()) {
      if (rule.integer()) tag.putInt(rule.id(), rules.number(rule));
      else tag.putBoolean(rule.id(), rules.enabled(rule));
    }
    world.put(TAG, tag);
  }
  private static void requireType(NbtCompound compound, String key, int type) {
    for (Object value : compound.values()) {
      NbtElement element = (NbtElement) value;
      if (element.getKey().equals(key) && element.getType() == type) return;
    }
    throw new IllegalArgumentException("Invalid or missing game-rule data: " + key + ". Original world is preserved.");
  }
  private static RuleSet legacy(NbtCompound tag) {
    if (tag.contains("PowerBetaCommandWorldId")) requireType(tag, "PowerBetaCommandWorldId", 8);
    if (tag.contains("PowerBetaWorldEditAccess")) {
      requireType(tag, "PowerBetaWorldEditAccess", 3);
      int access = tag.getInt("PowerBetaWorldEditAccess");
      if (access < 0 || access > 2) throw new IllegalArgumentException("Invalid old world-edit access. Original world is preserved.");
    }
    for (String key : new String[]{"PowerBetaCheatsEnabled", "PowerBetaKeepInventory", "PowerBetaDaylightCycle",
        "PowerBetaWeatherCycle", "PowerBetaHostileSpawning", "PowerBetaPassiveSpawning"}) {
      if (!tag.contains(key)) continue;
      requireType(tag, key, 1);
      if (tag.getByte(key) != 0 && tag.getByte(key) != 1)
        throw new IllegalArgumentException("Invalid old world rule: " + key + ". Original world is preserved.");
    }
    JsonObject old = new JsonObject();
    old.addProperty("world", tag.getString("PowerBetaCommandWorldId"));
    old.addProperty("cheats", tag.getBoolean("PowerBetaCheatsEnabled"));
    old.addProperty("keepInventory", tag.getBoolean("PowerBetaKeepInventory"));
    old.addProperty("editing", tag.getInt("PowerBetaWorldEditAccess"));
    for (var mapping : java.util.Map.of("daylight", "PowerBetaDaylightCycle", "weather", "PowerBetaWeatherCycle",
        "hostile", "PowerBetaHostileSpawning", "passive", "PowerBetaPassiveSpawning").entrySet())
      old.addProperty(mapping.getKey(), !tag.contains(mapping.getValue()) || tag.getBoolean(mapping.getValue()));
    JsonObject config = PowerConfig.document();
    return LegacyWorldRules.convert(old, config.has("legacyWorldRules") ? config.getAsJsonObject("legacyWorldRules") : new JsonObject());
  }
}
