package local.luke.power.rules;

import com.google.gson.*;
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
    assertNull(GameRule.find("sleep_ambushes"));
    assertNull(GameRule.find("sleepAmbushes"));
    for (GameRule rule : new GameRule[]{PLAYERS_SLEEPING_PERCENTAGE, EXPLOSION_DROP_CHANCE}) {
      for (int n : new int[]{0, 37, 100}) assertEquals(n, rule.parse(Integer.toString(n)).getAsInt());
      for (String n : new String[]{"-1", "101", "1.5", "NaN"}) assertThrows(IllegalArgumentException.class, () -> rule.parse(n));
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
