package local.luke.power.storage;

import com.google.gson.*;
import local.luke.power.rules.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SprintRuleConfigTest {
  @Test void oldGlobalSprintCannotEnableEitherCreationPresetAndTuningIsRetained() {
    JsonObject source = JsonParser.parseString("""
        {"schemaVersion":7,"settings":{"sprint":{"enabled":true,"activation":2,
        "speed":145,"stamina":true,"dynamicFov":true},"gameRules":{
        "survival":{"version":3,"values":{"allow_creative_mode":false}},
        "cheats":{"version":3,"values":{"allow_creative_mode":true}}}},
        "legacyWorldRules":{"gameplay":{"fireSpread":false}}}
        """).getAsJsonObject();
    JsonObject original = source.deepCopy();
    JsonObject converted = ConfigVersions.convert(source);
    assertEquals(original, source);
    assertEquals(source.getAsJsonObject("settings").get("sprint"),
        converted.getAsJsonObject("settings").get("sprint"));
    assertEquals(source.get("legacyWorldRules"), converted.get("legacyWorldRules"));
    JsonObject presets = converted.getAsJsonObject("settings").getAsJsonObject("gameRules");
    for (String name : new String[]{"survival", "cheats"}) {
      RuleSet rules = RuleSet.read(presets.getAsJsonObject(name), new RuleSet());
      assertFalse(rules.enabled(GameRule.ALLOW_SPRINTING));
      assertEquals(name.equals("cheats"), rules.enabled(GameRule.ALLOW_CREATIVE_MODE));
    }
    assertEquals(converted, ConfigVersions.convert(converted));
  }

  @Test void explicitSprintPresetsSurviveNormalizationAndGlobalPreferenceChanges() {
    RuleSet survival = new RuleSet();
    survival.set(GameRule.ALLOW_SPRINTING, true);
    JsonObject settings = new JsonObject(), presets = new JsonObject(), source = new JsonObject();
    presets.add("survival", survival.document());
    presets.add("cheats", RuleSet.cheats().document());
    settings.add("gameRules", presets);
    source.addProperty("schemaVersion", ConfigVersions.CURRENT);
    source.add("settings", settings);
    for (boolean enabled : new boolean[]{true, false, true}) {
      JsonObject sprint = new JsonObject();
      sprint.addProperty("enabled", enabled);
      settings.add("sprint", sprint);
      JsonObject persisted = ConfigVersions.convert(source).getAsJsonObject("settings").getAsJsonObject("gameRules");
      assertTrue(RuleSet.read(persisted.getAsJsonObject("survival"), new RuleSet()).enabled(GameRule.ALLOW_SPRINTING));
      assertFalse(RuleSet.read(persisted.getAsJsonObject("cheats"), RuleSet.cheats()).enabled(GameRule.ALLOW_SPRINTING));
    }
  }
}
