package local.luke.power.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import local.luke.power.input.Chord;

class ControlKeySearchTest {
  @Test void modifierQueriesIgnoreCaseAndSpacingWithoutMatchingOtherBindings() {
    assertTrue(ControlKeySearch.score(" cOnTrOl + ", "Ctrl+Shift+S", 0, null) < FuzzySearch.NONE);
    assertEquals(FuzzySearch.NONE, ControlKeySearch.score("ctrl+", "S", 0, null));
    assertEquals(FuzzySearch.NONE, ControlKeySearch.score("ctrl+", "LCONTROL", 0, null));
    assertTrue(ControlKeySearch.score("mouse 4", "Alt+Mouse 4", 0, null) < FuzzySearch.NONE);
  }
  @Test void capturedHotkeysRequireTheWholeChordNotJustItsBaseKey() {
    int ctrlS = new Chord(31, Chord.CTRL).encoded();
    assertEquals(0, ControlKeySearch.score("anything", "", ctrlS, ctrlS));
    assertEquals(FuzzySearch.NONE, ControlKeySearch.score("s", "S", 31, ctrlS));
    assertEquals(FuzzySearch.NONE, ControlKeySearch.score("s", "Ctrl+Shift+S", new Chord(31, 3).encoded(), ctrlS));
    assertEquals(0, ControlKeySearch.score("", "", -98, -98));
    assertEquals(FuzzySearch.NONE, ControlKeySearch.score("", "", -99, -98));
  }
}
