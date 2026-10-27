package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class MusicSearchTest {
  @Test void exactAndLiteralMatchesRankBeforeFuzzyMatches() {
    var source = List.of("Melom", "A melon", "Melon music", "Melon", "No songs");
    assertEquals(List.of("Melon", "Melon music", "A melon", "Melom"),
        MusicSearch.filter(source, "melon", value -> value));
  }

  @Test void acceptsAccentsAbbreviationsAndSmallTyposWithoutChangingTiedOrder() {
    assertEquals(0, MusicSearch.score("cafe", "Café"));
    assertTrue(MusicSearch.score("am", "Ambient music") < MusicSearch.NONE);
    assertTrue(MusicSearch.score("nigth", "Night") < MusicSearch.NONE);
    assertEquals(List.of("Night one", "Night two"),
        MusicSearch.filter(List.of("Night one", "Night two"), "night", value -> value));
  }

  @Test void blankSearchKeepsSavedOrderAndUnmatchedNamesStayOut() {
    var names = List.of("Favorites", "Beta 1.7.3");
    assertSame(names, MusicSearch.filter(names, " ", value -> value));
    assertEquals(List.of(), MusicSearch.filter(names, "unrelated", value -> value));
  }
}
