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

  @Test void indexedMusicHeadingsMatchMixedGroupsAndEveryScrollBoundary() {
    var rows = new java.util.ArrayList<Row>();
    int y = 0;
    for (int children : new int[]{0, 7, 8, 1, 18, 0, 9}) {
      rows.add(new Row(true, y, 20)); y += 20;
      for (int i = 0; i < children; i++) {
        int height = i % 2 == 0 ? 24 : 40;
        rows.add(new Row(false, y, height)); y += height;
      }
    }
    MusicRowIndex index = MusicRowIndex.of(rows, Row::heading, Row::y, Row::height);
    for (int visibleHeight : new int[]{0, 20, 43, 44, 120, 300})
      for (int scroll = -1; scroll <= y + 1; scroll++) {
        StickyHeader expected = StickyHeader.at(rows, Row::heading, Row::y, Row::height,
            scroll, 50, 50 + visibleHeight);
        assertEquals(expected, index.sticky(scroll, 50, 50 + visibleHeight),
            "scroll=" + scroll + ", height=" + visibleHeight);
      }
  }

  @Test void musicViewportAndHitTestsUseTheSameHalfOpenRowBounds() {
    var rows = List.of(new Row(true, 0, 20), new Row(false, 20, 24), new Row(false, 50, 40));
    MusicRowIndex index = MusicRowIndex.of(rows, Row::heading, Row::y, Row::height);
    assertEquals(0, index.firstVisible(-1));
    assertEquals(0, index.firstVisible(19));
    assertEquals(1, index.firstVisible(20));
    assertEquals(2, index.firstVisible(44));
    assertEquals(3, index.firstVisible(90));
    assertEquals(0, index.limitVisible(0));
    assertEquals(1, index.limitVisible(20));
    assertEquals(2, index.limitVisible(44));
    assertEquals(2, index.limitVisible(50));
    assertEquals(3, index.limitVisible(51));
    for (int y = -1; y <= 91; y++) {
      int expected = -1;
      for (int i = 0; i < rows.size(); i++) {
        Row row = rows.get(i);
        if (y >= row.y && y < row.y + row.height) { expected = i; break; }
      }
      assertEquals(expected, index.at(y), "y=" + y);
    }
  }

  @Test void indexedMusicLookupsNeverRevisitOrRetainTheSourceRows() {
    var rows = new java.util.ArrayList<>(ROWS);
    int[] reads = {0};
    MusicRowIndex index = MusicRowIndex.of(rows, row -> { reads[0]++; return row.heading; },
        row -> { reads[0]++; return row.y; }, row -> { reads[0]++; return row.height; });
    int buildReads = reads[0];
    rows.clear();
    for (int i = 0; i < 100; i++) {
      assertEquals(1, index.firstVisible(30));
      assertEquals(9, index.limitVisible(216));
      assertEquals(1, index.at(30));
      assertEquals(new StickyHeader(0, 50, 24), index.sticky(30, 50, 210));
    }
    assertEquals(buildReads, reads[0]);
    MusicRowIndex empty = MusicRowIndex.of(rows, Row::heading, Row::y, Row::height);
    assertEquals(0, empty.firstVisible(0)); assertEquals(0, empty.limitVisible(0));
    assertEquals(-1, empty.at(0)); assertNull(empty.sticky(30, 50, 210));
  }
}
