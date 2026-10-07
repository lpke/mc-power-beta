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
}
