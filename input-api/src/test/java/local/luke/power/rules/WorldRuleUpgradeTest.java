package local.luke.power.rules;

import com.google.gson.*;
import local.luke.power.storage.ConfigVersions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static local.luke.power.rules.GameRule.*;

class WorldRuleUpgradeTest {
  private JsonObject json(String text) { return JsonParser.parseString(text).getAsJsonObject(); }

  @Test void newWorldDefaultsDifferFromOldNativeTrampling() {
    for (RuleSet rules : new RuleSet[]{new RuleSet(), RuleSet.cheats()}) {
      assertFalse(rules.enabled(PLAYER_CROP_TRAMPLING));
      assertFalse(rules.enabled(MOB_CROP_TRAMPLING));
      assertTrue(rules.enabled(LAVA_DESTROYS_ITEMS));
      assertTrue(rules.enabled(CACTUS_DESTROYS_ITEMS));
    }
    RuleSet old = WorldRuleUpgrade.defaults(new JsonObject());
    assertTrue(old.enabled(PLAYER_CROP_TRAMPLING));
    assertTrue(old.enabled(MOB_CROP_TRAMPLING));
  }

  @Test void conversionFreezesEffectiveImmunitiesAndDoesNotMutateItsInput() {
    JsonObject source = json("{\"schemaVersion\":4,\"settings\":{\"gameplay\":{\"lavaProofItems\":true,\"cactusProofItems\":false}}}");
    JsonObject before = source.deepCopy(), converted = ConfigVersions.convert(source);
    assertEquals(before, source);
    assertEquals(5, converted.get("schemaVersion").getAsInt());
    assertFalse(converted.getAsJsonObject("settings").getAsJsonObject("gameplay").has("lavaProofItems"));
    RuleSet rules = WorldRuleUpgrade.defaults(converted.getAsJsonObject("worldRuleUpgrade"));
    assertFalse(rules.enabled(LAVA_DESTROYS_ITEMS));
    assertTrue(rules.enabled(CACTUS_DESTROYS_ITEMS));
    assertEquals(converted, ConfigVersions.convert(converted));
  }

  @Test void conditionalFootwearNeedsAnExplicitChoiceAndPreservesEvidence() {
    JsonObject evidence = json("{\"power_mechanics:config\":{\"MOB_CONFIG\":{\"disableTrampleFarmlandWithLeatherBoots\":true}}}");
    JsonObject original = evidence.deepCopy();
    assertThrows(IllegalArgumentException.class, () -> WorldRuleUpgrade.defaults(evidence));
    assertEquals(original, evidence);
    evidence.addProperty("playerCropTrampling", false);
    assertFalse(WorldRuleUpgrade.defaults(evidence).enabled(PLAYER_CROP_TRAMPLING));
    evidence.addProperty("playerCropTrampling", true);
    assertTrue(WorldRuleUpgrade.defaults(evidence).enabled(PLAYER_CROP_TRAMPLING));
  }

  @Test void oldSavedRulesWinOverConversionDefaultsAndSurviveGson289() {
    JsonObject evidence = json("{\"gameplay\":{\"lavaProofItems\":true}}");
    RuleSet rules = RuleSet.readValues(json("{\"mob_crop_trampling\":false,\"keep_inventory\":true}"), WorldRuleUpgrade.defaults(evidence));
    assertFalse(rules.enabled(MOB_CROP_TRAMPLING));
    assertTrue(rules.enabled(PLAYER_CROP_TRAMPLING));
    assertTrue(rules.enabled(KEEP_INVENTORY));
    assertFalse(rules.enabled(LAVA_DESTROYS_ITEMS));
    JsonObject saved = new Gson().fromJson(new Gson().toJson(rules.document()), JsonObject.class);
    assertEquals(rules, RuleSet.read(saved, new RuleSet()));
  }

  @Test void customCreationPresetsKeepEditedRulesAndAddOnlyDeclaredDefaults() {
    JsonObject source = json("{\"schemaVersion\":4,\"settings\":{\"gameRules\":{\"survival\":{\"version\":1,\"values\":{\"mob_crop_trampling\":true}}}}}");
    JsonObject preset = ConfigVersions.convert(source).getAsJsonObject("settings").getAsJsonObject("gameRules").getAsJsonObject("survival");
    RuleSet rules = RuleSet.read(preset, new RuleSet());
    assertEquals(2, preset.get("version").getAsInt());
    assertTrue(rules.enabled(MOB_CROP_TRAMPLING));
    assertFalse(rules.enabled(PLAYER_CROP_TRAMPLING));
    assertTrue(rules.enabled(LAVA_DESTROYS_ITEMS));
  }
}
