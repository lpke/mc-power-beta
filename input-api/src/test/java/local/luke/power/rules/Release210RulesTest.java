package local.luke.power.rules;

import com.google.gson.*;
import local.luke.power.storage.ConfigVersions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static local.luke.power.rules.GameRule.*;

class Release210RulesTest {
  private JsonObject json(String text) { return new Gson().fromJson(text, JsonObject.class); }

  @Test void defaultsAndAliasesHaveOneOwner() {
    RuleSet rules = new RuleSet();
    assertTrue(rules.enabled(ALLOW_SLEEP));
    assertFalse(rules.enabled(ALLOW_FLIGHT));
    assertTrue(rules.enabled(GHAST_EXPLOSION_FIRE));
    assertFalse(rules.enabled(LIGHTNING_STRIKE_FIRE));
    assertEquals(100, rules.number(EXPLOSION_DROP_CHANCE));
    assertSame(SLEEP_NIGHTMARES, GameRule.find("sleep_ambushes"));
    assertSame(SLEEP_NIGHTMARES, GameRule.find("sleepAmbushes"));
    for (GameRule rule : new GameRule[]{PLAYERS_SLEEPING_PERCENTAGE, EXPLOSION_DROP_CHANCE}) {
      for (int n : new int[]{0, 37, 100}) assertEquals(n, rule.parse(Integer.toString(n)).getAsInt());
      for (String n : new String[]{"-1", "101", "1.5", "NaN"}) assertThrows(IllegalArgumentException.class, () -> rule.parse(n));
    }
  }
  @Test void oldWorldsRetainDisabledSleepingNightmaresAndOtherRules() {
    JsonObject document = json("{\"version\":2,\"values\":{\"sleep_ambushes\":false,\"players_sleeping_percentage\":125,\"keep_inventory\":true}}");
    JsonObject before = document.deepCopy();
    RuleSet rules = RuleSet.read(document, new RuleSet());
    assertFalse(rules.enabled(ALLOW_SLEEP));
    assertFalse(rules.enabled(SLEEP_NIGHTMARES));
    assertTrue(rules.enabled(KEEP_INVENTORY));
    assertFalse(rules.enabled(ALLOW_FLIGHT));
    assertEquals(100, rules.number(PLAYERS_SLEEPING_PERCENTAGE));
    assertEquals(before, document);
    assertEquals(rules, RuleSet.read(json(new Gson().toJson(rules.document())), new RuleSet()));
    assertThrows(IllegalArgumentException.class, () -> RuleSet.read(json("{\"version\":2,\"values\":{\"sleep_ambushes\":false,\"sleep_nightmares\":true}}"), new RuleSet()));
  }
  @Test void configConversionFreezesEvidenceAndCopiesSprintPreferencesOnce() {
    JsonObject source = json("{\"schemaVersion\":5,\"worldRuleUpgrade\":{\"playerCropTrampling\":false},\"settings\":{\"power_controls:tweaks\":{\"disableSleeping\":true},\"power_mechanics:config\":{\"EXPLOSION_AND_FIRE_CONFIG\":{\"chanceBlocksDropWhenExploded\":0.37,\"disableGhastExplosionCausingFire\":true,\"disableLightningStrikeCausingFire\":false}},\"creative\":{\"sprintToggle\":false,\"sprintMultiplier\":173},\"gameRules\":{\"survival\":{\"version\":2,\"values\":{\"advance_weather\":false}}}}}");
    JsonObject before = source.deepCopy(), converted = ConfigVersions.convert(source);
    assertEquals(before, source);
    assertEquals(6, converted.get("schemaVersion").getAsInt());
    assertEquals(source.get("worldRuleUpgrade"), converted.get("worldRuleUpgrade"));
    JsonObject settings = converted.getAsJsonObject("settings");
    assertFalse(settings.getAsJsonObject("power_controls:tweaks").has("disableSleeping"));
    assertFalse(settings.getAsJsonObject("creative").get("spectatorSprintToggle").getAsBoolean());
    assertEquals(173, settings.getAsJsonObject("power_camera:config").get("sprintMultiplier").getAsInt());
    RuleSet preset = RuleSet.read(settings.getAsJsonObject("gameRules").getAsJsonObject("survival"), new RuleSet());
    assertFalse(preset.enabled(ALLOW_SLEEP));
    assertFalse(preset.enabled(GHAST_EXPLOSION_FIRE));
    assertTrue(preset.enabled(LIGHTNING_STRIKE_FIRE));
    assertFalse(preset.enabled(ADVANCE_WEATHER));
    assertEquals(37, preset.number(EXPLOSION_DROP_CHANCE));
    settings.getAsJsonObject("creative").addProperty("sprintMultiplier", 250);
    assertEquals(converted, ConfigVersions.convert(converted));
    assertEquals(173, settings.getAsJsonObject("power_camera:config").get("sprintMultiplier").getAsInt());
    RuleSet later = WorldRuleUpgrade210.defaults(converted.getAsJsonObject("worldRuleUpgrade210"), new RuleSet());
    assertFalse(later.enabled(ALLOW_SLEEP));
    assertEquals(37, later.number(EXPLOSION_DROP_CHANCE));
  }
  @Test void retiredRainSuppressionAndSchemaThreeCreationDefaultsKeepTheirRestrictions() {
    for (int version : new int[]{3, 4, 5}) {
      JsonObject source = json("{\"schemaVersion\":" + version + ",\"settings\":{\"power_controls:tweaks\":{\"disableSleeping\":true},\"power_environment:config\":{\"WEATHER_CONFIG\":{\"disableRain\":true}}}}");
      JsonObject converted = ConfigVersions.convert(source);
      JsonObject evidence = converted.getAsJsonObject("worldRuleUpgrade210");
      JsonObject values = WorldRuleUpgrade210.values(json("{\"advance_weather\":true}"), 2, evidence);
      assertFalse(values.get("advance_weather").getAsBoolean());
      assertEquals(converted, ConfigVersions.convert(converted));
      if (version == 3) {
        RuleSet preset = RuleSet.read(converted.getAsJsonObject("settings").getAsJsonObject("gameRules").getAsJsonObject("survival"), new RuleSet());
        assertFalse(preset.enabled(ALLOW_SLEEP)); assertFalse(preset.enabled(ADVANCE_WEATHER));
      }
    }
  }

  @Test void malformedEvidenceNeverSilentlyResetsPreferences() {
    for (String chance : new String[]{"-0.1", "1.01", "0.3333", "\"0.5\"", "null"}) {
      JsonObject source = json("{\"schemaVersion\":5,\"settings\":{\"power_mechanics:config\":{\"EXPLOSION_AND_FIRE_CONFIG\":{\"chanceBlocksDropWhenExploded\":" + chance + "}}}}");
      JsonObject before = source.deepCopy();
      assertThrows(RuntimeException.class, () -> ConfigVersions.convert(source));
      assertEquals(before, source);
    }
  }
  @Test void survivalFlightNeedsExplicitGrantAndPermissionWhileCreativeKeepsItsOwnAbility() {
    for (boolean creative : new boolean[]{false, true}) for (boolean permission : new boolean[]{false, true})
      for (boolean preference : new boolean[]{false, true}) for (int override : new int[]{-1, 0, 1}) {
        assertFalse(FlightAccess.allowed(true, creative, permission, preference, override));
        boolean expected = override != 0 && (creative ? override == 1 || preference : permission && override == 1);
        assertEquals(expected, FlightAccess.allowed(false, creative, permission, preference, override));
      }
  }
}
