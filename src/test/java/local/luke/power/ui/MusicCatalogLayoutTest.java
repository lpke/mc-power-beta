package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MusicCatalogLayoutTest {
  @Test void nameUseAndActionsHaveSeparateBoundsAtEverySupportedWidth() {
    for (int width = 180; width <= 1600; width++) {
      var layout = MusicCatalogLayout.at(31, width, 34, 70);
      assertTrue(layout.nameWidth() >= 140);
      assertTrue(layout.activeX() + 2 < layout.nameX());
      assertTrue(layout.useX() >= 31);
      assertEquals(layout.useX() + layout.useWidth() + 4, layout.duplicateX());
      assertEquals(layout.duplicateX() + layout.duplicateWidth() + 4, layout.deleteX());
      assertEquals(31 + width, layout.deleteX() + 20);
      assertEquals(8, layout.nameX() - 31);
      assertEquals(34, layout.useWidth());
      assertTrue(layout.controlsY(90) + 18 <= 90 + layout.rowHeight());
      if (layout.inline()) assertEquals(layout.nameX() + layout.nameWidth() + 4, layout.useX());
      else {
        assertTrue(layout.controlsY(90) >= 90 + 23);
        assertEquals(layout.right(), layout.nameX() + layout.nameWidth());
      }
    }
  }
}
