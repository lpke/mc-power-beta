package local.luke.power.rules;

import com.google.gson.*;
import java.math.BigDecimal;

/** Frozen schema-5 preferences supply only rules absent from older world metadata. */
public final class WorldRuleUpgrade210 {
  private WorldRuleUpgrade210() {}

  public static RuleSet defaults(JsonObject evidence, RuleSet defaults) {
    RuleSet rules = defaults.copy();
    rules.set(GameRule.ALLOW_SLEEP, !bool(object(evidence, "power_controls:tweaks"), "disableSleeping", false));
    JsonObject explosions = object(object(evidence, "power_mechanics:config"), "EXPLOSION_AND_FIRE_CONFIG");
    rules.set(GameRule.GHAST_EXPLOSION_FIRE, !bool(explosions, "disableGhastExplosionCausingFire", false));
    rules.set(GameRule.LIGHTNING_STRIKE_FIRE, !bool(explosions, "disableLightningStrikeCausingFire", true));
    JsonElement chance = explosions.get("chanceBlocksDropWhenExploded");
    if (chance != null) {
      if (!chance.isJsonPrimitive() || !chance.getAsJsonPrimitive().isNumber())
        throw new IllegalArgumentException("Invalid old explosion drop chance. Original data is preserved.");
      rules.set(GameRule.EXPLOSION_DROP_CHANCE, chance.getAsBigDecimal().multiply(BigDecimal.valueOf(100)).intValueExact());
    }
    if (bool(object(object(evidence, "power_environment:config"), "WEATHER_CONFIG"), "disableRain", false))
      rules.set(GameRule.ADVANCE_WEATHER, false);
    return rules;
  }
  public static JsonObject values(JsonObject source, int version, JsonObject evidence) {
    JsonObject values = RuleSet.upgradeValues(source, version);
    if (version < 3 && bool(object(object(evidence, "power_environment:config"), "WEATHER_CONFIG"), "disableRain", false))
      values.addProperty("advance_weather", false);
    return values;
  }
  private static JsonObject object(JsonObject parent, String key) {
    return parent.has(key) ? parent.getAsJsonObject(key) : new JsonObject();
  }
  private static boolean bool(JsonObject parent, String key, boolean fallback) {
    if (!parent.has(key)) return fallback;
    JsonElement value = parent.get(key);
    if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean())
      throw new IllegalArgumentException("Invalid old preference: " + key);
    return value.getAsBoolean();
  }
}
