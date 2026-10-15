package local.luke.power.rules;

import com.google.gson.*;

/** Defaults only for worlds saved before format 2. Evidence is frozen at config conversion. */
public final class WorldRuleUpgrade {
  private WorldRuleUpgrade() {}

  public static RuleSet defaults(JsonObject evidence) {
    RuleSet rules = new RuleSet();
    JsonObject gameplay = object(evidence, "gameplay");
    rules.set(GameRule.LAVA_DESTROYS_ITEMS, !bool(gameplay, "lavaProofItems"));
    rules.set(GameRule.CACTUS_DESTROYS_ITEMS, !bool(gameplay, "cactusProofItems"));
    JsonObject mobs = object(object(evidence, "power_mechanics:config"), "MOB_CONFIG");
    boolean allProtected = bool(mobs, "disableTramplingFarmland");
    if (!allProtected && !evidence.has("playerCropTrampling")
        && (bool(mobs, "disableTrampleFarmlandWhenBarefoot") || bool(mobs, "disableTrampleFarmlandWithLeatherBoots")))
      throw new IllegalArgumentException("Cannot convert player_crop_trampling: the old protection depends on footwear. Set worldRuleUpgrade.playerCropTrampling to true or false in the configuration before converting. Original world is preserved.");
    rules.set(GameRule.PLAYER_CROP_TRAMPLING, evidence.has("playerCropTrampling")
        ? bool(evidence, "playerCropTrampling") : !allProtected);
    rules.set(GameRule.MOB_CROP_TRAMPLING, !allProtected);
    return rules;
  }

  private static JsonObject object(JsonObject parent, String key) {
    return parent.has(key) ? parent.getAsJsonObject(key) : new JsonObject();
  }
  private static boolean bool(JsonObject parent, String key) {
    if (!parent.has(key)) return false;
    JsonElement value = parent.get(key);
    if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean())
      throw new IllegalArgumentException("Invalid old world preference: " + key);
    return value.getAsBoolean();
  }
}
