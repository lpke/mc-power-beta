package local.luke.power.creative.inventory;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CreativeGridTest {
  @Test void eightRowsFitAndScrollingUsesCompleteRows() {
    for (int count = 0; count <= 64; count++) assertEquals(0, CreativeGrid.maxScroll(count));
    assertEquals(8, CreativeGrid.maxScroll(65));
    assertEquals(8, CreativeGrid.maxScroll(72));
    assertEquals(16, CreativeGrid.maxScroll(73));
    assertEquals(64, CreativeGrid.CAPACITY);
  }
  @Test void tabsRemainVisibleAtMinimumGuiHeightAndGridStopsBeforeHotbar() {
    for (int height : new int[]{240, 270, 480, 1080}) {
      int base = (height - 166) / 2;
      assertTrue(CreativeGrid.tabTop(base) >= 0);
      assertTrue(CreativeGrid.tabHeight(base) >= 20);
      int lastRow = base - CreativeGrid.EXTRA_HEIGHT + 14 + (CreativeGrid.ROWS - 1) * 18;
      assertEquals(22, base + 142 - lastRow, "Match survival row-to-hotbar spacing");
      assertTrue(CreativeGrid.tabTop(base) + CreativeGrid.tabHeight(base) <= base - CreativeGrid.EXTRA_HEIGHT + 3);
    }
  }

  @Test void catalogueBodyProtectsTheExtraRowAndMarginsFromOutsideDrops() {
    for (int[] size : new int[][] {{320, 240}, {640, 420}}) {
      int x = (size[0] - 176) / 2, y = (size[1] - 166) / 2;
      assertFalse(CreativeGrid.outsideBody(x, y - 20, x, y, 176, 166, false));
      assertFalse(CreativeGrid.outsideBody(x + 175, y + 165, x, y, 176, 166, false));
      assertTrue(CreativeGrid.outsideBody(x - 1, y, x, y, 176, 166, false));
      assertTrue(CreativeGrid.outsideBody(x, y - 21, x, y, 176, 166, false));
      assertTrue(CreativeGrid.outsideBody(x + 176, y, x, y, 176, 166, false));
      assertTrue(CreativeGrid.outsideBody(x, y + 166, x, y, 176, 166, false));
    }
  }

  @Test void destroySlotBorderIsPartOfTheWindowOnlyWhenVisible() {
    assertFalse(CreativeGrid.outsideBody(78, 237, 100, 100, 176, 166, true));
    assertFalse(CreativeGrid.outsideBody(99, 265, 100, 100, 176, 166, true));
    assertTrue(CreativeGrid.outsideBody(78, 237, 100, 100, 176, 166, false));
    assertTrue(CreativeGrid.outsideBody(77, 237, 100, 100, 176, 166, true));
  }
}
