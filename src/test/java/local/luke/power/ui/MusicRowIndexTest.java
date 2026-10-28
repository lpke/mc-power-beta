package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MusicRowIndexTest {
  private record Row(boolean heading, int y, int height) {}
  private static MusicRowIndex index(List<Row> rows) {
    return MusicRowIndex.of(rows, Row::heading, Row::y, Row::height);
  }

  @Test void wheelAlignmentExposesAnEntireSongInTheCompactGroupedViewport() {
    List<Row> rows = new ArrayList<>();
    rows.add(new Row(true, 0, 20));
    for (int i = 0; i < 12; i++) rows.add(new Row(false, 20 + i * 24, 24));
    var index = index(rows);
    int scroll = index.scrollByRow(0, 1);
    assertEquals(20, scroll);
    assertNull(index.sticky(scroll, 0, 27));
    Row firstSong = rows.get(index.firstVisible(scroll));
    assertFalse(firstSong.heading());
    assertEquals(0, firstSong.y() - scroll);
    assertTrue(firstSong.y() + firstSong.height() - scroll <= 27);
    assertEquals(44, index.scrollByRow(scroll, 1));
    assertEquals(0, index.scrollByRow(scroll, -1));
  }

  @Test void stepsToTheNearestBoundaryInEitherDirectionAndStopsAtTheEnds() {
    var index = index(List.of(new Row(true, 0, 20), new Row(false, 20, 24), new Row(false, 44, 24)));
    assertEquals(44, index.scrollByRow(21, 1));
    assertEquals(20, index.scrollByRow(43, -1));
    assertEquals(20, index.scrollByRow(44, -1));
    assertEquals(0, index.scrollByRow(0, -1));
    assertEquals(44, index.scrollByRow(44, 1));
    assertEquals(21, index.scrollByRow(21, 0));
  }

  @Test void emptyAndUngroupedListsRemainSafe() {
    assertEquals(0, index(List.of()).scrollByRow(0, 1));
    var index = index(List.of(new Row(false, 0, 24), new Row(false, 24, 24)));
    assertEquals(24, index.scrollByRow(0, 1));
    assertEquals(0, index.scrollByRow(24, -1));
  }
}
