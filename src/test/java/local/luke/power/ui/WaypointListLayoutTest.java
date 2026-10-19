package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class WaypointListLayoutTest {
  @Test void listControlsStayInsideCenteredContentFromNarrowToUltrawide() {
    for (int[] size : new int[][] {{320,240}, {480,270}, {640,360}, {800,600}, {1720,720}, {1920,540}, {540,960}}) {
      var layout = WaypointListLayout.at(size[0], size[1]);
      assertEquals(size[0], layout.left() * 2 + layout.width(), 1);
      assertTrue(layout.width() <= size[1] * 16 / 9 - 16);
      assertTrue(layout.left() >= 8);
      assertTrue(layout.right() <= size[0] - 8);
      assertTrue(layout.searchWidth() >= 100);
      assertEquals(layout.sortX() - 4, layout.left() + layout.searchWidth());
      assertEquals(layout.filterX() - 4, layout.sortX() + layout.sortWidth());
      assertEquals(layout.right(), layout.filterX() + layout.filterWidth());
      assertEquals(layout.createX() - 4, layout.left() + layout.setWidth());
      assertEquals(layout.right(), layout.createX() + layout.createWidth());
      assertTrue(layout.actionWidth() >= 70);
      assertTrue(layout.actionX(3) + layout.actionWidth() <= layout.right());
      assertTrue(layout.listBottom() > layout.listTop());
      for (int count : new int[] {1, 12, 8192}) {
        int visible = layout.setRows(size[1], count);
        assertTrue(visible > 0 && visible <= count);
        assertTrue(69 + visible * 20 < size[1]);
      }
    }
  }
}
