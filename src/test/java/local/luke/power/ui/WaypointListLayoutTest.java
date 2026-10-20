package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class WaypointListLayoutTest {
  @Test void listControlsStayInsideCenteredContentFromNarrowToUltrawide() {
    for (int[] size : new int[][] {{320,240}, {480,270}, {640,360}, {800,600}, {1720,720}, {1920,540}, {540,960}}) {
      var layout = WaypointListLayout.at(size[0], size[1]);
      assertEquals(size[0], layout.left() * 2 + layout.width(), 1);
      assertTrue(layout.width() <= size[1] * 16 / 9 - 16);
      assertTrue(layout.width() <= 480);
      assertTrue(layout.searchWidth() <= 400);
      assertTrue(layout.left() >= 8);
      assertTrue(layout.right() <= size[0] - 8);
      assertTrue(layout.searchWidth() >= 100);
      assertEquals(layout.sortX() - 4, layout.left() + layout.searchWidth());
      assertEquals(layout.filterX() - 4, layout.sortX() + layout.sortWidth());
      assertEquals(layout.right(), layout.filterX() + layout.filterWidth());
      assertEquals(layout.deleteX() - 4, layout.left() + layout.setWidth());
      assertEquals(layout.createX() - 4, layout.deleteX() + layout.deleteWidth());
      assertEquals(layout.right(), layout.createX() + layout.createWidth());
      assertTrue(layout.setWidth() >= 140, "set selector retains room beside both actions");
      assertEquals(layout.left(), layout.newX());
      assertTrue(layout.newX() + layout.footerWidth() + 4 <= layout.doneX());
      assertEquals(layout.right(), layout.doneX() + layout.footerWidth());
      assertTrue(layout.footerWidth() >= 140, "New waypoint fits beside Done");
      assertTrue(layout.listBottom() > layout.listTop());
      assertEquals(size[1] - 32, layout.listBottom());
      assertTrue(layout.sortWidth() >= 96, "Sort: Distance fits without crowding search");
      for (int count : new int[] {1, 12, 8192}) {
        int visible = layout.setRows(size[1], count);
        assertTrue(visible > 0 && visible <= count);
        assertTrue(69 + visible * 20 < size[1]);
      }
    }
  }

  @Test void wideWindowsKeepTheSameControlWidthsAndUseExtraSpaceAsMargins() {
    var normal = WaypointListLayout.at(640, 360);
    for (int[] size : new int[][] {{800,600}, {1720,720}, {1920,540}, {2560,1080}}) {
      var wide = WaypointListLayout.at(size[0], size[1]);
      assertEquals(480, wide.width());
      assertEquals(normal.searchWidth(), wide.searchWidth());
      assertEquals(normal.setWidth(), wide.setWidth());
      assertTrue(wide.left() > normal.left());
      assertEquals(size[0], wide.left() * 2 + wide.width(), 1);
    }
  }

  @Test void shortAndPortraitWindowsStillRespectTheirAvailableContentArea() {
    assertEquals(304, WaypointListLayout.at(320, 240).width());
    assertEquals(304, WaypointListLayout.at(1280, 180).width());
    assertEquals(480, WaypointListLayout.at(540, 960).width());
  }
}
