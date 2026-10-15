package local.luke.power.rules;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static local.luke.power.rules.GameRule.*;

class LegacyWorldRulesTest {
  private JsonObject json(String value) { return JsonParser.parseString(value).getAsJsonObject(); }
  @Test void legacySurvivalNeverGainsAccessAndDormantCheatRulesStayInactive() {
    RuleSet rules = LegacyWorldRules.convert(json("{\"cheats\":false,\"daylight\":false,\"keepInventory\":true}"), new JsonObject());
    assertEquals(WorldRuleUpgrade.defaults(new JsonObject()), rules);
  }
  @Test void oldCheatsPreserveKeepInventoryInsteadOfAdoptingNewPreset() {
    RuleSet rules = LegacyWorldRules.convert(json("{\"cheats\":true}"), new JsonObject());
    for (GameRule rule : GameRule.values()) if (rule.access()) assertTrue(rules.enabled(rule));
    assertFalse(rules.enabled(KEEP_INVENTORY));
  }
  @Test void worldOverridesAndGlobalMasterAreAppliedBeforeMerging() {
    JsonObject world = json("{\"cheats\":true,\"world\":\"a\"}");
    JsonObject globals = json("{\"commandAccess\":{\"rules\":{\"give\":\"DISABLED\"},\"worlds\":{\"a\":{\"give\":\"ALLOW\"}},\"modeAccess\":{\"spectator\":false}}}");
    RuleSet rules = LegacyWorldRules.convert(world, globals);
    assertTrue(rules.enabled(ALLOW_ITEM_SPAWNING)); assertFalse(rules.enabled(ALLOW_SPECTATOR_MODE));
    globals.getAsJsonObject("commandAccess").addProperty("enabled", false);
    rules = LegacyWorldRules.convert(world, globals);
    assertFalse(rules.enabled(ALLOW_ITEM_SPAWNING)); assertFalse(rules.enabled(ALLOW_CREATIVE_MODE));
    assertTrue(rules.enabled(ALLOW_WORLD_EDITING)); assertTrue(rules.enabled(ALLOW_DEATH_RECOVERY));
  }
  @Test void conflictingGroupedAccessNeverSilentlyWidensOrShrinksPermissions() {
    JsonObject globals = json("{\"commandAccess\":{\"rules\":{\"heal\":\"DISABLED\"}}}");
    assertThrows(IllegalArgumentException.class, () -> LegacyWorldRules.convert(json("{\"cheats\":true}"), globals));
    assertEquals(WorldRuleUpgrade.defaults(new JsonObject()), LegacyWorldRules.convert(json("{\"cheats\":false}"), globals));
  }
  @Test void allExplosionItemProtectionMovesToTheWorldAndDoesNotAffectBlocks() {
    RuleSet rules = LegacyWorldRules.convert(new JsonObject(), json("{\"gameplay\":{\"explosionProofItems\":true,\"fireSpread\":false}}"));
    assertFalse(rules.enabled(EXPLOSIONS_DESTROY_ITEMS)); assertFalse(rules.enabled(FIRE_SPREAD));
    assertTrue(rules.enabled(TNT_EXPLODES)); assertTrue(rules.enabled(CREEPER_GRIEFING));
  }
}
