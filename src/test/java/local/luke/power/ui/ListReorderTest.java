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

  @Test void musicPressesNeedDeliberateMovementAndDoNotBecomeClicksAgain() {
    for (MusicListDrag.Kind kind : MusicListDrag.Kind.values()) {
      MusicListDrag drag = new MusicListDrag();
      drag.begin(kind, this, List.of("a"), List.of(new MusicListDrag.Slot("a", 0, 20)), 0, 100, 100);
      assertTrue(drag.active()); assertFalse(drag.dragging());
      drag.update(103, 103, true);
      assertTrue(drag.active()); assertFalse(drag.dragging());
      drag.update(107, 100, true); assertFalse(drag.dragging());
      drag.update(108, 100, true); assertTrue(drag.dragging());
      drag.update(100, 100, true); assertTrue(drag.dragging());
      drag.cancel(); assertFalse(drag.active()); assertFalse(drag.dragging());
    }
  }

  @Test void diagonalMovementUsesDistanceAndFocusLossReleasesTheSource() {
    MusicListDrag drag = new MusicListDrag();
    drag.begin(MusicListDrag.Kind.GROUPS, this, List.of("a"),
        List.of(new MusicListDrag.Slot("a", 0, 20)), 0, 100, 100);
    drag.update(105, 105, true); assertFalse(drag.dragging());
    drag.update(106, 106, true); assertTrue(drag.dragging());
    drag.update(106, 106, false);
    assertFalse(drag.active()); assertFalse(drag.dragging());
    assertNull(drag.owner()); assertTrue(drag.original().isEmpty());
  }

  @Test void musicDropSlotsRetainSnapshotGeometryAcrossVariableHeightRows() {
    var ids = new ArrayList<>(List.of("a", "b", "c"));
    var slots = new ArrayList<>(List.of(new MusicListDrag.Slot("a", 0, 20),
        new MusicListDrag.Slot("b", 120, 20), new MusicListDrag.Slot("c", 180, 40)));
    MusicListDrag drag = new MusicListDrag();
    drag.begin(MusicListDrag.Kind.GROUPS, this, ids, slots, 0, 10, 10);
    ids.clear(); slots.clear();
    assertEquals(List.of("a", "b", "c"), drag.original());
    assertEquals(0, drag.slot(-1)); assertEquals(0, drag.slot(9));
    assertEquals(1, drag.slot(10)); assertEquals(1, drag.slot(129));
    assertEquals(2, drag.slot(130)); assertEquals(2, drag.slot(199));
    assertEquals(3, drag.slot(200)); assertEquals(3, drag.slot(900));
    assertEquals(0, drag.lineY(0)); assertEquals(120, drag.lineY(1));
    assertEquals(180, drag.lineY(2)); assertEquals(220, drag.lineY(3));
  }
}
