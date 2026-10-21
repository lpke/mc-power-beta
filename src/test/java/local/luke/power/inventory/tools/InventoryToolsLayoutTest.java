package local.luke.power.inventory.tools;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class InventoryToolsLayoutTest {
  @Test void headerFollowsShiftedPanelAndItsHitBoxUsesExactEdges() {
    var plain = InventoryToolsLayout.sort(600, 400, 176, 166, 0, true);
    var shifted = InventoryToolsLayout.sort(600, 400, 176, 166, 77, true);
    assertEquals(plain.x() + 77, shifted.x()); assertEquals(plain.y(), shifted.y());
    assertTrue(shifted.contains(shifted.x(), shifted.y())); assertFalse(shifted.contains(shifted.x() + shifted.width(), shifted.y()));
    var second = InventoryToolsLayout.second(600, 400, 176, 166, 77); assertTrue(shifted.x() + shifted.width() < second.x());
  }
  @Test void largeChestHeaderStaysVisibleWithoutMovingNativeSlots() {
    var header = InventoryToolsLayout.sort(320, 240, 176, 222, 0, true); assertEquals(14, header.y()); assertEquals(10, header.height());
    assertTrue(header.x() >= (320 - 176) / 2);
    assertTrue(header.y() + header.height() < (240 - 222) / 2 + 36);
  }
  @Test void compactHeadersShareTenPixelBoundsAndTwelvePixelSpacing() {
    for (int width : new int[]{320, 427, 600}) for (int offset : new int[]{0, 77}) for (boolean second : new boolean[]{false, true}) {
      var sort = InventoryToolsLayout.sort(width, 240, 176, 166, offset, second);
      var recipe = InventoryToolsLayout.recipe(width, 240, 176, 166, offset, second);
      assertEquals(10, sort.width()); assertEquals(10, sort.height());
      assertEquals(sort.width(), recipe.width()); assertEquals(sort.height(), recipe.height());
      assertEquals(12, sort.x() - recipe.x()); assertEquals(sort.y(), recipe.y());
      assertFalse(recipe.contains(recipe.x() + 10, recipe.y() + 5));
      var last = second ? InventoryToolsLayout.second(width, 240, 176, 166, offset) : sort;
      assertEquals((width + 176) / 2 + offset - 7, last.x() + last.width());
      if (second) assertEquals(12, last.x() - sort.x());
    }
  }
  @Test void everyVisibleButtonCombinationPacksAgainstTheRightInset() {
    for (int mask = 0; mask < 8; mask++) for (int width : new int[]{320, 427, 600}) for (int offset : new int[]{0, 77}) {
      boolean sort = (mask & 1) != 0, second = (mask & 2) != 0, recipe = (mask & 4) != 0;
      var header = InventoryToolsLayout.header(width, 240, 176, 166, offset, sort, second, recipe);
      assertEquals(sort, header.sort() != null); assertEquals(second, header.second() != null); assertEquals(recipe, header.recipe() != null);
      var visible = java.util.Arrays.asList(header.recipe(), header.sort(), header.second()).stream().filter(java.util.Objects::nonNull).toList();
      if (visible.isEmpty()) continue;
      int right = (width + 176) / 2 + offset - 7;
      assertEquals(right, visible.get(visible.size() - 1).x() + InventoryToolsLayout.BUTTON_SIZE);
      for (int i = 0; i < visible.size(); i++) {
        var button = visible.get(i);
        assertEquals(10, button.width()); assertEquals(10, button.height());
        assertTrue(button.contains(button.x(), button.y())); assertFalse(button.contains(button.x() + 10, button.y()));
        if (i > 0) {
          var previous = visible.get(i - 1);
          assertEquals(12, button.x() - previous.x()); assertEquals(previous.y(), button.y());
          assertFalse(previous.contains(button.x(), button.y()));
        }
      }
    }
  }
  @Test void modalStaysInsideNarrowScreensAndTracksBookOffsetWhenSpaceAllows() {
    for (int width : new int[]{320, 427, 600}) for (int offset : new int[]{0, 77}) {
      var box = InventoryToolsLayout.modal(width, 240, offset); assertTrue(box.x() >= 8); assertTrue(box.y() >= 4);
      assertTrue(box.x() + box.width() <= width - 8); assertTrue(box.y() + box.height() <= 236);
    }
    assertEquals(77, InventoryToolsLayout.modal(600, 400, 77).x() - InventoryToolsLayout.modal(600, 400, 0).x());
  }
}
