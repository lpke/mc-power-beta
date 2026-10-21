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
    var header = InventoryToolsLayout.sort(320, 240, 176, 222, 0, true); assertEquals(14, header.y()); assertEquals(14, header.height());
    assertTrue(header.x() >= (320 - 176) / 2);
    assertTrue(header.y() + header.height() < (240 - 222) / 2 + 36);
  }
  @Test void modalStaysInsideNarrowScreensAndTracksBookOffsetWhenSpaceAllows() {
    for (int width : new int[]{320, 427, 600}) for (int offset : new int[]{0, 77}) {
      var box = InventoryToolsLayout.modal(width, 240, offset); assertTrue(box.x() >= 8); assertTrue(box.y() >= 4);
      assertTrue(box.x() + box.width() <= width - 8); assertTrue(box.y() + box.height() <= 236);
    }
    assertEquals(77, InventoryToolsLayout.modal(600, 400, 77).x() - InventoryToolsLayout.modal(600, 400, 0).x());
  }
}
