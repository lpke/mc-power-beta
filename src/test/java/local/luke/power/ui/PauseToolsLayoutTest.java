package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class PauseToolsLayoutTest {
  @Test void defaultClusterCentersAboveBackAndShrinksWhenButtonsAreExcluded() {
    var back = new PauseToolsLayout.Box(220, 140, 200, 20);
    for (int count = 1; count <= 3; count++) {
      var row = PauseToolsLayout.at(640, 360, 0, count, back, List.of(back));
      assertEquals(320, row.x() + row.width() / 2);
      assertEquals(116, row.y()); assertEquals(count * 24 - 4, row.width());
      assertTrue(row.contains(row.x(), row.y()));
      assertFalse(row.contains(row.x() + row.width(), row.y()));
      assertFalse(row.overlaps(back));
    }
  }
  @Test void allAnchorsStayInBoundsAndAvoidNativeButtonsAndMusicAtNarrowSizes() {
    for (int[] size : List.of(new int[]{320, 180}, new int[]{640, 360}, new int[]{1280, 540})) {
      var back = new PauseToolsLayout.Box(size[0] / 2 - 100, size[1] / 4 + 12, 200, 20);
      var obstacles = List.of(back,
          new PauseToolsLayout.Box(back.x(), back.y() + 24, 200, 20),
          new PauseToolsLayout.Box(back.x(), back.y() + 48, 200, 20),
          new PauseToolsLayout.Box(back.x(), back.y() + 72, 200, 20),
          new PauseToolsLayout.Box(size[0] / 2 - 100, 36, 200, 16),
          new PauseToolsLayout.Box(size[0] / 2 - 100, 4, 200, 32));
      for (int position = 0; position < PauseToolsLayout.POSITIONS.size(); position++) {
        var row = PauseToolsLayout.at(size[0], size[1], position, 3, back, obstacles);
        assertTrue(row.x() >= 4 && row.x() + row.width() <= size[0] - 4);
        assertTrue(row.y() >= 4 && row.y() + row.height() <= size[1] - 4);
        assertTrue(obstacles.stream().noneMatch(row::overlaps), position + " at " + size[0]);
      }
    }
  }
  @Test void tightMenuTopMusicUsesTheExactTwentyPixelGapInsteadOfOverlappingTitleOrBack() {
    var back = new PauseToolsLayout.Box(60, 72, 200, 20);
    for (var music : List.of(new PauseToolsLayout.Box(60, 8, 200, 32),
        new PauseToolsLayout.Box(60, 4, 200, 48))) {
      var obstacles = List.of(new PauseToolsLayout.Box(60, 36, 200, 16), music, back,
          new PauseToolsLayout.Box(60, 96, 98, 20), new PauseToolsLayout.Box(162, 96, 98, 20),
          new PauseToolsLayout.Box(60, 120, 200, 20), new PauseToolsLayout.Box(60, 144, 200, 20));
      var tools = PauseToolsLayout.at(320, 180, 0, 3, back, obstacles);
      assertEquals(new PauseToolsLayout.Box(126, 52, 68, 20), tools);
      assertTrue(obstacles.stream().noneMatch(tools::overlaps));
      assertEquals(back.y(), tools.y() + tools.height());
      assertTrue(tools.contains(126, 52)); assertFalse(tools.contains(126, 72));
    }
  }
}
