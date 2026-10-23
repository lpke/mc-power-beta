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
      int lastRow = base - CreativeGrid.EXTRA_HEIGHT + CreativeGrid.ITEM_TOP + (CreativeGrid.ROWS - 1) * 18;
      assertEquals(22, base + 142 - lastRow, "Match survival row-to-hotbar spacing");
      assertTrue(CreativeGrid.tabTop(base) + CreativeGrid.tabHeight(base) <= base - CreativeGrid.EXTRA_HEIGHT + CreativeGrid.TITLE_TOP);
      assertTrue(CreativeGrid.tabIconTop(base) >= CreativeGrid.tabTop(base));
      assertTrue(CreativeGrid.tabIconTop(base) + 16 <= base - CreativeGrid.EXTRA_HEIGHT + CreativeGrid.TITLE_TOP - 1);
    }
  }

  @Test void catalogueBodyProtectsTheExtraRowAndMarginsFromOutsideDrops() {
    for (int[] size : new int[][] {{320, 240}, {640, 420}}) {
      int x = (size[0] - 176) / 2, y = (size[1] - 166) / 2;
      assertFalse(CreativeGrid.outsideBody(x, y - CreativeGrid.EXTRA_HEIGHT, x, y, 176, 166, false));
      assertFalse(CreativeGrid.outsideBody(x + 175, y + 165, x, y, 176, 166, false));
      assertTrue(CreativeGrid.outsideBody(x - 1, y, x, y, 176, 166, false));
      assertTrue(CreativeGrid.outsideBody(x, y - CreativeGrid.EXTRA_HEIGHT - 1, x, y, 176, 166, false));
      assertTrue(CreativeGrid.outsideBody(x + 176, y, x, y, 176, 166, false));
      assertTrue(CreativeGrid.outsideBody(x, y + 166, x, y, 176, 166, false));
    }
  }

  @Test void destroySlotBorderIsPartOfTheWindowOnlyWhenVisible() {
    assertFalse(CreativeGrid.outsideBody(80, 236, 100, 100, 176, 166, true));
    assertFalse(CreativeGrid.outsideBody(99, 265, 100, 100, 176, 166, true));
    assertTrue(CreativeGrid.outsideBody(80, 236, 100, 100, 176, 166, false));
    assertTrue(CreativeGrid.outsideBody(79, 236, 100, 100, 176, 166, true));
  }
  @Test void sideFrameHitTestingMatchesEveryVisibilityCombination() {
    // A combined frame starts at y+114; either slot alone starts at y+136.
    for (boolean destroy : new boolean[]{false, true}) for (boolean offhand : new boolean[]{false, true}) {
      int top = destroy && offhand ? 214 : 236;
      assertEquals(!(destroy || offhand), CreativeGrid.outsideBody(80, top + 1, 100, 100, 176, 166, destroy, offhand));
      assertTrue(CreativeGrid.outsideBody(79, top + 1, 100, 100, 176, 166, destroy, offhand));
      assertTrue(CreativeGrid.outsideBody(80, top - 1, 100, 100, 176, 166, destroy, offhand));
      assertEquals(!(destroy || offhand), CreativeGrid.outsideBody(99, 265, 100, 100, 176, 166, destroy, offhand));
      assertTrue(CreativeGrid.outsideBody(80, 266, 100, 100, 176, 166, destroy, offhand));
      assertEquals(!(destroy && offhand), CreativeGrid.outsideBody(80, 215, 100, 100, 176, 166, destroy, offhand));
    }
  }

}
