package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class InheritedColorControlTest {
  @Test void modeAndColorShareBoundariesAtNarrowAndWideWidths() {
    for (int width : new int[] {20, 44, 60, 100, 240}) {
      var inherited = InheritedColorControl.at(10, width, false);
      assertEquals(width, inherited.modeWidth()); assertEquals(0, inherited.colorWidth());
      assertFalse(inherited.colorAt(10 + width - 1));
      var custom = InheritedColorControl.at(10, width, true);
      assertTrue(custom.modeWidth() > 0 && custom.colorWidth() > 0);
      assertTrue(custom.colorX() >= 10 + custom.modeWidth());
      assertEquals(10 + width, custom.colorX() + custom.colorWidth());
      assertFalse(custom.colorAt(custom.colorX() - 1));
      assertTrue(custom.colorAt(custom.colorX()));
      assertTrue(custom.colorAt(10 + width - 1));
      assertFalse(custom.colorAt(10 + width));
    }
  }
}
