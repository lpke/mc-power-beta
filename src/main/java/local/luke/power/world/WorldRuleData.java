package local.luke.power.world;

import com.google.gson.*;
import local.luke.power.rules.*;
import net.minecraft.nbt.*;

/** Strict current-format native NBT boundary, validated before world state is published. */
public final class WorldRuleData {
  public static final String TAG = "GameRules";
  private WorldRuleData() {}

  public static RuleSet read(NbtCompound world) {
    if (!world.contains(TAG)) throw new IllegalArgumentException("World game rules are missing. Upgrade this existing world with Power Beta 4.4.1 first. Original world is preserved.");
    requireType(world, TAG, 10);
    NbtCompound tag = world.getCompound(TAG);
    requireType(tag, "version", 3);
    int version = tag.getInt("version");
    if (version != RuleSet.VERSION)
      throw new IllegalArgumentException("World game rules version " + version + " is unsupported. Supported version: " + RuleSet.VERSION + ". Upgrade older worlds with Power Beta 4.4.1 first, or use the release that saved this world. Original world is preserved.");
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
}
