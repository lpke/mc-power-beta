package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FuzzySearchTest {
  @Test
  void findsTyposAbbreviationsAndTransposedLetters() {
    for (String query : new String[] {"brighness", "brihgtness", "brght"})
      assertTrue(FuzzySearch.score(query, "Brightness", "") < FuzzySearch.NONE, query);
    assertTrue(FuzzySearch.score("renderdistance", "Render distance", "") < FuzzySearch.NONE);
    assertTrue(FuzzySearch.score("rndrdst", "Render distance", "") < FuzzySearch.NONE);
    assertTrue(FuzzySearch.score("rd", "Render distance", "") < FuzzySearch.NONE);
  }

  @Test
  void ranksNamesBeforeDescriptionsAndRequiresEveryWord() {
    assertTrue(
        FuzzySearch.score("music", "Music", "")
            < FuzzySearch.score("music", "Sound", "Music volume"));
    assertEquals(FuzzySearch.NONE, FuzzySearch.score("music potato", "Music", "Sound volume"));
    assertEquals(FuzzySearch.NONE, FuzzySearch.score("zzzzzz", "Brightness", "Light"));
    assertTrue(FuzzySearch.score("light video", "Brightness", "Video lighting") < FuzzySearch.NONE);
    assertEquals(0, FuzzySearch.score("ete", "Été", ""));
  }
}
