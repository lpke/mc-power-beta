package local.luke.power.creative.inventory;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class InventoryPreviewTest {
  @Test void nestedRenderingAndFailuresRestoreWorldVisibility() {
    assertFalse(InventoryPreview.active());
    InventoryPreview.render(() -> {
      assertTrue(InventoryPreview.active());
      assertThrows(IllegalStateException.class, () -> InventoryPreview.render(() -> {
        assertTrue(InventoryPreview.active());
        throw new IllegalStateException("native renderer failed");
      }));
      assertTrue(InventoryPreview.active());
    });
    assertFalse(InventoryPreview.active());
    assertThrows(IllegalStateException.class, () -> InventoryPreview.render(() -> {
      throw new IllegalStateException("native renderer failed");
    }));
    assertFalse(InventoryPreview.active());
  }
}
