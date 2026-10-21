package local.luke.power.input;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class InventoryWheelTest {
  @AfterEach void clear() { InventoryWheel.clear(); }

  @Test void mixedMinimapCatalogAndSlotEventsHaveOneOwnerEach() {
    Object screen = new Object();
    InventoryWheel.event(screen, true, 120, false);
    InventoryWheel.finishEvent(screen, true);
    InventoryWheel.event(screen, true, -120, true);
    InventoryWheel.event(screen, true, 240, false);
    assertEquals(240, InventoryWheel.takeEvent(screen, true));
    InventoryWheel.finishEvent(screen, true);
    assertEquals(120, InventoryWheel.take(screen, true));
    assertEquals(0, InventoryWheel.take(screen, true));
  }

  @Test void pointerMotionCannotReplayAnEarlierWheelOverANewSlot() {
    Object screen = new Object();
    InventoryWheel.event(screen, true, 120, false);
    InventoryWheel.finishEvent(screen, false);
    InventoryWheel.event(screen, true, 0, false);
    assertEquals(0, InventoryWheel.takeEvent(screen, true));
    assertEquals(0, InventoryWheel.take(screen, true));
  }

  @Test void subNotchAndOpposingAmountsRetainTheirNativeCounts() {
    Object screen = new Object();
    for (int amount : new int[] {1, 7, 120, -30}) {
      InventoryWheel.event(screen, true, amount, false);
      InventoryWheel.finishEvent(screen, true);
    }
    assertEquals(98, InventoryWheel.take(screen, true));
  }

  @Test void focusAndScreenChangesDiscardPendingEvents() {
    Object screen = new Object(), next = new Object();
    InventoryWheel.event(screen, true, 120, false);
    assertEquals(0, InventoryWheel.take(screen, false));
    assertEquals(0, InventoryWheel.take(screen, true));
    InventoryWheel.event(screen, true, 120, false);
    assertEquals(0, InventoryWheel.take(next, true));
    InventoryWheel.event(next, true, 120, false);
    InventoryWheel.clear();
    assertEquals(0, InventoryWheel.take(next, true));
  }

  @Test void renderingBeforeQueuedDispatchCannotConsumeFutureWheelEvents() {
    Object screen = new Object();
    assertEquals(0, InventoryWheel.take(screen, true));
    InventoryWheel.event(screen, true, 120, true);
    InventoryWheel.finishEvent(screen, true);
    assertEquals(0, InventoryWheel.take(screen, true));
    InventoryWheel.event(screen, true, 120, false);
    InventoryWheel.finishEvent(screen, true);
    assertEquals(120, InventoryWheel.take(screen, true));
  }
}
