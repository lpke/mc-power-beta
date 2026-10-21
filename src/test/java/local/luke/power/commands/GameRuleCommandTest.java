package local.luke.power.commands;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GameRuleCommandTest {
  @Test void booleanCompletionsOfferBothAcceptedSpellingsAndFilterByPrefix() {
    assertEquals(Set.of("true", "false", "on", "off"), Set.of(GameRuleCommand.suggestions(2, "", "/gamerule fire_spread ")));
    assertEquals(Set.of("n", "ff"), Set.of(GameRuleCommand.suggestions(2, "o", "/gamerule doFireTick o")));
    assertArrayEquals(new String[]{"f"}, GameRuleCommand.suggestions(2, "of", "/gamerule fire_spread of"));
    assertEquals(Set.of("0", "100"), Set.of(GameRuleCommand.suggestions(2, "", "/gamerule players_sleeping_percentage ")));
  }
}
