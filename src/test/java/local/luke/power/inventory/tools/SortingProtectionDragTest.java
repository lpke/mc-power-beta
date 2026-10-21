package local.luke.power.inventory.tools;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class SortingProtectionDragTest {
  @Test void selectionClickAndMotionWithinStartingCellNeverChangeProtection() {
    var gesture = new SortingProtectionDrag(); List<Integer> writes = new ArrayList<>();
    gesture.begin(9, 0, false); gesture.move(9, (slot, value) -> writes.add(slot));
    assertTrue(writes.isEmpty()); gesture.release(9, 0, (slot, value) -> writes.add(slot)); assertTrue(writes.isEmpty());
  }
  @Test void dragPaintsStartingCellOnlyAfterLeavingAndUsesItsOriginalProtection() {
    var gesture = new SortingProtectionDrag(); Map<Integer, Boolean> writes = new LinkedHashMap<>();
    gesture.begin(9, 0, true); gesture.move(9, writes::put); assertTrue(writes.isEmpty());
    gesture.move(10, writes::put); gesture.move(11, writes::put); gesture.release(10, 0, writes::put);
    assertEquals(Map.of(9, false, 10, false, 11, false), writes);
  }
  @Test void leavingGridStartsPaintingWithoutInventingInvalidSlotsAndCanReturn() {
    var gesture = new SortingProtectionDrag(); Map<Integer, Boolean> writes = new LinkedHashMap<>();
    gesture.begin(35, 0, false); gesture.move(-1, writes::put); assertEquals(Map.of(35, true), writes);
    gesture.release(34, 0, writes::put); assertEquals(Map.of(35, true, 34, true), writes);
  }
  @Test void rightClickTogglesOnceAtReleaseAndCanceledOrUnrelatedReleasesDoNotPaint() {
    var gesture = new SortingProtectionDrag(); List<Integer> writes = new ArrayList<>();
    gesture.begin(12, 1, false); gesture.release(12, 0, (slot, value) -> writes.add(slot)); assertTrue(writes.isEmpty());
    gesture.release(12, 1, (slot, value) -> writes.add(slot)); assertEquals(List.of(12), writes);
    gesture.begin(13, 0, false); gesture.cancel(); gesture.release(14, 0, (slot, value) -> writes.add(slot)); assertEquals(List.of(12), writes);
  }
}
