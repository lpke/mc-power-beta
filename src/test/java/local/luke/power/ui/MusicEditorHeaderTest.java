package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MusicEditorHeaderTest {
  @Test void localBackStaysBeforeTheNameWithoutTakingTheCompactActionRow() {
    for (int width = 180; width <= 1600; width++) {
      var h = MusicEditorHeader.withBack(31, 90, width, 74, 101, 32);
      assertEquals(31, h.backX());
      assertEquals(44, h.backWidth());
      assertEquals(h.backX() + h.backWidth() + 4, h.nameX());
      assertTrue(h.nameWidth() >= 32);
      assertTrue(h.nameX() + h.nameWidth() <= 31 + width);
      assertTrue(h.viewX() + h.viewWidth() <= 31 + width);
      if (!h.inline()) {
        assertEquals(31, h.addX());
        assertEquals(112, h.actionsY());
        assertEquals(44, h.height());
      }
    }
    var compact = MusicEditorHeader.withBack(106, 121, 200, 67, 84, 32);
    assertFalse(compact.inline());
    assertEquals(152, compact.nameWidth());
    assertEquals(143, compact.actionsY());
  }

  @Test void headerUsesOneRowWhenTheNameAndMeasuredControlsFit() {
    for (int width = 180; width <= 1600; width++) {
      var h = MusicEditorHeader.at(31, 90, width, 74, 101);
      assertEquals(31, h.nameX()); assertTrue(h.nameWidth() >= 100);
      assertTrue(h.viewX() + h.viewWidth() <= 31 + width);
      assertEquals(h.addX() + h.addWidth() + 4, h.viewX());
      if (h.inline()) {
        assertEquals(90, h.actionsY()); assertEquals(22, h.height());
        assertEquals(h.nameX() + h.nameWidth() + 4, h.addX());
        assertEquals(31 + width, h.viewX() + h.viewWidth());
      } else {
        assertEquals(112, h.actionsY()); assertEquals(44, h.height());
        assertEquals(31, h.addX()); assertEquals(width, h.nameWidth());
      }
    }
    assertFalse(MusicEditorHeader.at(0, 0, 282, 74, 101).inline());
    assertTrue(MusicEditorHeader.at(0, 0, 283, 74, 101).inline());
    assertEquals(22, MusicEditorHeader.at(0, 0, 288, 74, 101).height());
  }

  @Test void protectedViewsNeedNoSpaceForAnAddOrSaveButton() {
    var h = MusicEditorHeader.at(8, 90, 180, 0, 101);
    assertEquals(0, h.addWidth());
    assertTrue(h.inline());
    assertEquals(75, h.nameWidth());
    assertEquals(h.nameX() + h.nameWidth() + 4, h.viewX());
    assertEquals(188, h.viewX() + h.viewWidth());
  }

  @Test void compactEditorKeepsActionsBesideItsScrollableName() {
    for (int[] controls : new int[][]{{67, 84}, {40, 88}, {67, 60}}) {
      var header = MusicEditorHeader.at(106, 121, 200, controls[0], controls[1], 32);
      assertTrue(header.inline());
      assertTrue(header.nameWidth() >= 32);
      assertEquals(22, header.height());
      assertEquals(306, header.viewX() + header.viewWidth());
    }
  }

  @Test void longerAddBrowserLabelsStayInsideSmallScreens() {
    var h = MusicEditorHeader.at(10, 90, 180, 104, 90);
    assertFalse(h.inline());
    assertEquals(44, h.height());
    assertEquals(180, h.nameWidth());
    assertEquals(190, h.viewX() + h.viewWidth());
    assertTrue(h.addWidth() > 0);
  }
}
