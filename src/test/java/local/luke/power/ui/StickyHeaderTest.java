package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class StickyHeaderTest {
  private record Row(boolean heading, int y, int height) {}
  private static final List<Row> ROWS = List.of(
      new Row(true, 0, 24), new Row(false, 24, 24), new Row(false, 48, 40),
      new Row(false, 88, 24), new Row(true, 112, 24), new Row(false, 136, 24),
      new Row(false, 160, 24), new Row(false, 184, 24));

  private StickyHeader at(List<Row> rows, int scroll) {
    return StickyHeader.at(rows, Row::heading, Row::y, Row::height, scroll, 50, 210);
  }

  @Test void pinsOnlyTheCurrentGroupAndSwitchesWhenTheNextHeadingArrives() {
    assertNull(at(ROWS, 0));
    assertEquals(new StickyHeader(0, 50, 24), at(ROWS, 30));
    assertNull(at(ROWS, 112));
    assertEquals(new StickyHeader(4, 50, 24), at(ROWS, 130));
  }

  @Test void finalItemPushesTheHeadingAwayBeforeItCanCoverThatItem() {
    var pinned = at(ROWS, 76);
    assertEquals(38, pinned.y());
    assertEquals(50 + 88 - 76, pinned.y() + pinned.height());
    assertFalse(pinned.contains(49, 50));
    assertTrue(pinned.contains(50, 50));
    assertFalse(pinned.contains(62, 50));
    assertNull(at(ROWS, 88));
    assertNull(at(ROWS, 200));
  }

  @Test void singleItemAndCollapsedGroupsNeverCoverContent() {
    var rows = List.of(new Row(true, 0, 24), new Row(false, 24, 40));
    assertNull(at(rows, 8));
    assertNull(at(rows, 24));
    assertNull(at(List.of(new Row(true, 0, 24), new Row(true, 24, 24)), 15));
    assertNull(at(List.of(new Row(false, 0, 24)), 10));
    assertNull(at(List.of(), 50));
    assertNull(StickyHeader.at(ROWS, Row::heading, Row::y, Row::height, 30, 50, 70));
    assertNull(StickyHeader.at(ROWS, Row::heading, Row::y, Row::height, 30, 50, 95));
  }
}
