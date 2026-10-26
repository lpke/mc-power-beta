package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MusicEditorHeaderTest {
  @Test void headerControlsFitAndNameKeepsUsableWidthAcrossTheBreakpoint() {
    for (int width = 180; width <= 1600; width++) {
      var h = MusicEditorHeader.at(31, 90, width);
      assertEquals(31, h.nameX()); assertTrue(h.nameWidth() >= 120);
      assertTrue(h.saveX() >= h.nameX() + h.nameWidth() + 4);
      assertTrue(h.viewX() + h.actionWidth() <= 31 + width);
      if (h.inline()) {
        assertEquals(90, h.actionsY()); assertEquals(22, h.height());
        assertEquals(h.saveX() + 44, h.addX());
        assertEquals(31 + width, h.viewX() + h.actionWidth());
      } else {
        assertEquals(112, h.actionsY()); assertEquals(44, h.height());
        assertEquals(31, h.addX()); assertEquals(31 + width, h.saveX() + 40);
      }
    }
    assertFalse(MusicEditorHeader.at(0, 0, 379).inline());
    assertTrue(MusicEditorHeader.at(0, 0, 380).inline());
  }
}
