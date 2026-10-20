package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class OptionsFooterLayoutTest {
  @Test void applyRetainsTextPaddingInNarrowAndWidePanels() {
    for (int span : new int[] {200, 240, 320, 360, 469, 470, 960}) {
      for (int text : new int[] {51, 54, 66}) {
        var layout = OptionsFooterLayout.at(span, span + 100, text);
        assertTrue(layout.width(1) >= text + 16);
        assertEquals(layout.x(0) + layout.width(0) + 4, layout.x(1));
        assertEquals(layout.x(1) + layout.width(1) + 4, layout.x(2));
        assertEquals(span + 92, layout.x(2) + layout.width(2));
        assertTrue(layout.x(0) >= 100, "actions remain inside the panel");
      }
    }
  }
}
