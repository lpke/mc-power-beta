package local.luke.power.autowalk;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class InventoryLookDragTest {
  private final Object screen = new Object(), world = new Object(), player = new Object();

  @Test void onlyThePressCanStartLookingAndMouseUpEndsIt() {
    var drag = new InventoryLookDrag();
    assertFalse(drag.press(screen, world, player, false));
    assertFalse(drag.update(screen, world, player, true, true));
    assertTrue(drag.press(screen, world, player, true));
    assertTrue(drag.update(screen, world, player, true, true));
    assertFalse(drag.update(screen, world, player, false, true));
    assertFalse(drag.update(screen, world, player, true, true));
  }

  @Test void lostEligibilityCannotRestartWhileHeld() {
    var drag = new InventoryLookDrag(); drag.press(screen, world, player, true);
    assertFalse(drag.update(screen, world, player, true, false));
    assertFalse(drag.update(screen, world, player, true, true));
    assertTrue(drag.press(screen, world, player, true));
  }

  @Test void screenWorldAndPlayerChangesEndOwnership() {
    for (int changed = 0; changed < 3; changed++) {
      var drag = new InventoryLookDrag(); drag.press(screen, world, player, true);
      assertFalse(drag.update(changed == 0 ? new Object() : screen,
          changed == 1 ? new Object() : world, changed == 2 ? new Object() : player, true, true));
      assertFalse(drag.update(screen, world, player, true, true));
    }
  }
}
