package local.luke.power.inventory.tools;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SavedHotbarKeyStateTest {
  @Test void repeatedNativeReadsAndContainerDispatchRunOneAction() {
    var keys = new SavedHotbarKeyState(); var calls = new AtomicInteger();
    for (int i = 0; i < 20; i++) assertTrue(keys.event(12, 2, true, false, () -> { calls.incrementAndGet(); return true; }));
    assertEquals(1, calls.get());
  }
  @Test void consumedPressRetainsOwnershipWhenActivatorReleasesFirst() {
    var keys = new SavedHotbarKeyState();
    assertTrue(keys.event(1, 10, true, false, () -> true));
    assertTrue(keys.event(2, 10, true, true, () -> fail("A repeat must not invoke the action")));
    assertFalse(keys.event(3, 10, false, false, () -> fail("Release must not invoke the action")));
    assertFalse(keys.event(4, 10, true, false, () -> false));
  }
  @Test void numberReleaseAllowsAnotherPresetPressWhileActivatorStaysHeld() {
    var keys = new SavedHotbarKeyState(); var calls = new AtomicInteger();
    assertTrue(keys.event(1, 2, true, false, () -> { calls.incrementAndGet(); return true; }));
    assertFalse(keys.event(2, 2, false, false, () -> true));
    assertTrue(keys.event(3, 2, true, false, () -> { calls.incrementAndGet(); return true; }));
    assertEquals(2, calls.get());
  }
  @Test void declinedAndUnrelatedEventsRetainNativeKeys() {
    var keys = new SavedHotbarKeyState();
    assertFalse(keys.event(1, 2, true, false, () -> false));
    assertFalse(keys.event(1, 2, true, false, () -> fail("A declined event is cached too")));
    assertFalse(keys.event(2, 61, true, false, () -> fail("F3 belongs to its normal owner")));
    assertFalse(keys.event(3, 3, true, true, () -> fail("Holding an activator mid-repeat must not create a new gesture")));
  }
  @Test void menuDrainedReleasesAreReconciledWithoutReplayingActions() {
    var keys = new SavedHotbarKeyState();
    assertTrue(keys.event(1, 2, true, false, () -> true));
    assertTrue(keys.event(2, 3, true, false, () -> true));
    keys.reconcile(key -> key == 3);
    assertFalse(keys.event(3, 2, true, false, () -> false));
    assertTrue(keys.event(4, 3, true, true, () -> fail("A still-held consumed press stays owned")));
  }
  @Test void releasingAnOlderOwnerDoesNotActivateSavedHotbarsHalfwayThroughItsNumberPress() {
    var keys = new SavedHotbarKeyState(); var calls = new AtomicInteger();
    assertFalse(keys.event(1, 2, true, false, () -> false));
    assertFalse(keys.event(1, 2, true, false, () -> fail("The older owner already won this event")));
    assertFalse(keys.event(2, 2, true, true, () -> fail("Releasing the old activator must not start a new saved gesture")));
    assertFalse(keys.event(3, 2, false, false, () -> true));
    assertTrue(keys.event(4, 2, true, false, () -> { calls.incrementAndGet(); return true; }));
    assertEquals(1, calls.get());
  }
}
