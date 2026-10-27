package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MusicCatalogLayoutTest {
  @Test void nameUseAndActionsHaveSeparateBoundsAtEverySupportedWidth() {
    for (int width = 180; width <= 1600; width++) {
      var layout = MusicCatalogLayout.at(31, width, 70);
      assertTrue(layout.useX() + layout.useWidth() < layout.nameX());
      assertTrue(layout.nameWidth() >= 100);
      assertTrue(layout.duplicateX() >= 31);
      assertEquals(layout.duplicateX() + layout.duplicateWidth() + 4, layout.deleteX());
      assertEquals(layout.useX() - 31, 31 + width - layout.deleteX() - 20);
      assertEquals(8, layout.useX() - 31);
      assertEquals(18, layout.useWidth());
      assertTrue(layout.controlsY(90) + 18 <= 90 + layout.rowHeight());
      if (layout.inline()) assertTrue(layout.nameX() + layout.nameWidth() < layout.duplicateX());
      else assertTrue(layout.controlsY(90) >= 90 + 23);
    }
  }
}
