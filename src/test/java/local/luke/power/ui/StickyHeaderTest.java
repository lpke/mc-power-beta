package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class StickyHeaderTest {
  private record Row(boolean heading, int y, int height) {}
  private static final List<Row> ROWS = java.util.stream.IntStream.range(0, 18)
      .mapToObj(i -> new Row(i % 9 == 0, i * 24, 24)).toList();

  private StickyHeader at(List<Row> rows, int scroll) {
    return StickyHeader.at(rows, Row::heading, Row::y, Row::height, scroll, 50, 210);
  }

  @Test void pinsOnlyTheCurrentGroupAndSwitchesWhenTheNextHeadingArrives() {
    assertNull(at(ROWS, 0));
    assertEquals(new StickyHeader(0, 50, 24), at(ROWS, 30));
    assertNull(at(ROWS, 216));
    assertEquals(new StickyHeader(9, 50, 24), at(ROWS, 230));
  }

  @Test void finalItemPushesTheHeadingAwayBeforeItCanCoverThatItem() {
    var pinned = at(ROWS, 180);
    assertEquals(38, pinned.y());
    assertEquals(50 + 192 - 180, pinned.y() + pinned.height());
    assertFalse(pinned.contains(49, 50));
    assertTrue(pinned.contains(50, 50));
    assertFalse(pinned.contains(62, 50));
    assertNull(at(ROWS, 192));
    assertNull(at(ROWS, 410));
  }

  @Test void groupsNeedAtLeastEightVisibleChildren() {
    var seven = java.util.stream.IntStream.range(0, 8)
        .mapToObj(i -> new Row(i == 0, i * 24, 24)).toList();
    assertNull(at(seven, 30));
    assertNotNull(at(ROWS, 30));
  }

  @Test void inGroupDividersDoNotCountAsSettingsOrHeadings() {
    record Line(boolean heading,boolean setting,int y,int height) {}
    var rows=new java.util.ArrayList<Line>();
    rows.add(new Line(true,false,0,24));
    for(int i=0;i<7;i++)rows.add(new Line(false,true,24+i*24,24));
    rows.add(new Line(false,false,192,13));
    assertNull(StickyHeader.at(rows,Line::heading,Line::setting,Line::y,Line::height,30,50,210));
    rows.add(new Line(false,true,205,24));
    assertNotNull(StickyHeader.at(rows,Line::heading,Line::setting,Line::y,Line::height,30,50,210));
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
