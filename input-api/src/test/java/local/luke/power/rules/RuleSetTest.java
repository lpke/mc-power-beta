package local.luke.power.rules;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static local.luke.power.rules.GameRule.*;

class RuleSetTest {
  @Test void aliasesAndDefaultsHaveOneOwner() {
    assertEquals(40, GameRule.values().length);
    assertEquals(12, java.util.Arrays.stream(GameRule.values()).filter(GameRule::access).count());
    RuleSet survival = new RuleSet(), cheats = RuleSet.cheats();
    for (GameRule rule : GameRule.values()) {
      assertSame(rule, GameRule.find(rule.id())); assertSame(rule, GameRule.find(rule.alias()));
      if (rule.access()) { assertFalse(survival.enabled(rule)); assertTrue(cheats.enabled(rule)); }
      else if (rule != KEEP_INVENTORY) assertEquals(survival.get(rule), cheats.get(rule));
    }
    assertFalse(survival.enabled(KEEP_INVENTORY)); assertTrue(cheats.enabled(KEEP_INVENTORY));
    assertTrue(survival.enabled(EXPLOSIONS_DESTROY_ITEMS));
    assertEquals(100, survival.number(PLAYERS_SLEEPING_PERCENTAGE));
    assertNull(GameRule.find("powerbeta:fire_spread"));
  }
  @Test void gson289RoundTripAndCopiesRemainIndependent() {
    RuleSet rules = RuleSet.cheats(); rules.set(FIRE_SPREAD, false); rules.set(PLAYERS_SLEEPING_PERCENTAGE, 75);
    JsonObject persisted = new Gson().fromJson(new Gson().toJson(rules.document()), JsonObject.class);
    RuleSet loaded = RuleSet.read(persisted, new RuleSet()); assertEquals(rules, loaded);
    RuleSet copied = loaded.copy(); loaded.set(FIRE_SPREAD, true); assertFalse(copied.enabled(FIRE_SPREAD));
    assertFalse(rules.enabled(FIRE_SPREAD));
  }
  @Test void missingFieldsUseDeclaredPresetDefaultsWithoutErasingChoices() {
    JsonObject doc = JsonParser.parseString("{\"version\":4,\"values\":{\"allow_world_editing\":false}}").getAsJsonObject();
    RuleSet rules = RuleSet.read(doc, RuleSet.cheats());
    assertFalse(rules.enabled(ALLOW_WORLD_EDITING)); assertTrue(rules.enabled(ALLOW_CREATIVE_MODE));
    assertTrue(rules.enabled(KEEP_INVENTORY)); assertTrue(rules.enabled(EXPLOSIONS_DESTROY_ITEMS));
  }
  @Test void badTypesUnknownNamesAndVersionsAreRejectedWithoutChangingInput() {
    for (String source : new String[]{"{\"version\":" + (RuleSet.VERSION + 1) + ",\"values\":{}}", "{\"version\":\"1\",\"values\":{}}",
        "{\"version\":4,\"values\":{\"fire_spread\":\"false\"}}", "{\"version\":4,\"values\":{\"fire_spread\":0}}",
        "{\"version\":4,\"values\":{\"players_sleeping_percentage\":-1}}", "{\"version\":4,\"values\":{\"players_sleeping_percentage\":1.5}}",
        "{\"version\":4,\"values\":{\"players_sleeping_percentage\":2147483648}}", "{\"version\":4,\"values\":{\"doFireTick\":true}}",
        "{\"version\":4,\"values\":{\"future_rule\":true}}"}) {
      JsonObject input = JsonParser.parseString(source).getAsJsonObject(), before = input.deepCopy();
      assertThrows(IllegalArgumentException.class, () -> RuleSet.read(input, new RuleSet()), source);
      assertEquals(before, input);
    }
  }
  @Test void commandParsingEnforcesPercentages() {
    assertFalse(FIRE_SPREAD.parse("FALSE").getAsBoolean());
    assertEquals(0, PLAYERS_SLEEPING_PERCENTAGE.parse("0").getAsInt());
    assertThrows(IllegalArgumentException.class, () -> PLAYERS_SLEEPING_PERCENTAGE.parse("101"));
    for (String value : new String[]{"true", "-1", "1.0", "1e2", "2147483648"})
      assertThrows(IllegalArgumentException.class, () -> PLAYERS_SLEEPING_PERCENTAGE.parse(value));
    assertThrows(IllegalArgumentException.class, () -> FIRE_SPREAD.parse("yes"));
  }
  @Test void booleanCommandAliasesStayBooleanInSavedData() {
    for (GameRule rule : GameRule.values()) {
      if (rule.integer()) {
        for (String value : new String[]{"on", "off", "true", "false"})
          assertThrows(IllegalArgumentException.class, () -> rule.parse(value));
        continue;
      }
      for (String value : new String[]{"true", "TRUE", "on", "On", "ON"}) {
        JsonPrimitive parsed = rule.parse(value);
        assertTrue(parsed.isBoolean()); assertTrue(parsed.getAsBoolean());
      }
      for (String value : new String[]{"false", "FALSE", "off", "Off", "OFF"}) {
        JsonPrimitive parsed = rule.parse(value);
        assertTrue(parsed.isBoolean()); assertFalse(parsed.getAsBoolean());
      }
      for (String value : new String[]{"yes", "no", "1", "0", "", " on", "off "})
        assertThrows(IllegalArgumentException.class, () -> rule.parse(value));
      assertThrows(IllegalArgumentException.class, () -> rule.checked(new JsonPrimitive("on")));
    }
  }
  @Test void groundSprintDefaultsOffAndRoundTripsIndependentlyOfOtherPermissions() {
    assertSame(ALLOW_SPRINTING, GameRule.find("allowSprinting"));
    assertTrue(ALLOW_SPRINTING.help().contains("/gamerule allow_sprinting"));
    for (RuleSet defaults : new RuleSet[]{new RuleSet(), RuleSet.cheats()}) {
      assertFalse(defaults.enabled(ALLOW_SPRINTING));
      JsonObject missingSprint = defaults.document();
      missingSprint.getAsJsonObject("values").remove("allow_sprinting");
      assertFalse(RuleSet.read(missingSprint, defaults).enabled(ALLOW_SPRINTING));
      RuleSet enabled = defaults.copy();
      enabled.set(ALLOW_SPRINTING, ALLOW_SPRINTING.parse("true"));
      RuleSet loaded = RuleSet.read(new Gson().fromJson(new Gson().toJson(enabled.document()), JsonObject.class), new RuleSet());
      assertTrue(loaded.enabled(ALLOW_SPRINTING));
      for (GameRule rule : GameRule.values()) if (rule != ALLOW_SPRINTING)
        assertEquals(defaults.get(rule), loaded.get(rule), rule.id());
      assertFalse(defaults.enabled(ALLOW_SPRINTING));
    }
  }
}
