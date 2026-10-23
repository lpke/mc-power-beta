package local.luke.power.input;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class InventoryTextInputTest {
  @AfterEach void clear() { InventoryTextInput.clear(); }
  @Test void disabledRecipeBookCannotReleaseFocusedCreativeSearch() {
    Object screen = new Object(), search = new Object(), recipe = new Object();
    InventoryTextInput.focus(screen, search, true);
    for (int i = 0; i < 30; i++) InventoryTextInput.focus(screen, recipe, false);
    InventoryTextInput.focus(screen, false);
    assertTrue(InventoryTextInput.active(screen));
    InventoryTextInput.focus(screen, search, false);
    assertFalse(InventoryTextInput.active(screen));
  }
  @Test void overlaysReleaseTheirOwnClaimsInEitherOrder() {
    Object screen = new Object(), first = new Object(), second = new Object();
    for (boolean reverse : new boolean[]{false, true}) {
      InventoryTextInput.focus(screen, first, true); InventoryTextInput.focus(screen, second, true);
      InventoryTextInput.focus(screen, reverse ? second : first, false);
      assertTrue(InventoryTextInput.active(screen));
      InventoryTextInput.focus(screen, reverse ? first : second, false);
      assertFalse(InventoryTextInput.active(screen));
    }
  }
  @Test void staleScreenReleaseCannotClearNewScreenOrLegacyClaim() {
    Object old = new Object(), next = new Object(), search = new Object();
    InventoryTextInput.focus(old, search, true); InventoryTextInput.focus(next, true);
    InventoryTextInput.focus(old, search, false);
    assertFalse(InventoryTextInput.active(old)); assertTrue(InventoryTextInput.active(next));
    InventoryTextInput.focus(next, new Object(), false); assertTrue(InventoryTextInput.active(next));
    InventoryTextInput.focus(next, false); assertFalse(InventoryTextInput.active(next));
  }
  @Test void globalScreenResetReleasesEveryClaim() {
    Object screen = new Object();
    InventoryTextInput.focus(screen, new Object(), true); InventoryTextInput.focus(screen, new Object(), true);
    InventoryTextInput.clear(); assertFalse(InventoryTextInput.active(screen));
  }
}
