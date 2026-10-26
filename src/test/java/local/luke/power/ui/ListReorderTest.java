package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ListReorderTest {
  @Test void draggingDownAndUpMovesOneItemWithoutChangingOtherRelativeOrder() {
    var tracks = new ArrayList<>(List.of("a", "missing", "b", "c"));
    assertTrue(ListReorder.move(tracks, 0, 4));
    assertEquals(List.of("missing", "b", "c", "a"), tracks);
    assertTrue(ListReorder.move(tracks, 3, 1));
    assertEquals(List.of("missing", "a", "b", "c"), tracks);
    assertFalse(ListReorder.move(tracks, 1, 2));
  }

  @Test void insertionGapsFollowTheScrolledRowsAndClampToTheList() {
    assertEquals(2, ListReorder.slot(55, 54, 52, 26, 8));
    assertEquals(3, ListReorder.slot(79, 54, 52, 26, 8));
    assertEquals(0, ListReorder.slot(-100, 54, 0, 26, 8));
    assertEquals(8, ListReorder.slot(900, 54, 0, 26, 8));
  }

  @Test void invalidDropsLeaveTheListIntact() {
    var tracks = new ArrayList<>(List.of("a", "b"));
    assertThrows(IllegalArgumentException.class, () -> ListReorder.move(tracks, 0, 3));
    assertEquals(List.of("a", "b"), tracks);
  }
}
