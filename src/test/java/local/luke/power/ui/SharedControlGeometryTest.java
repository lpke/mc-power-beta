package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

class SharedControlGeometryTest {
  @Test void sliderThumbAndPointerUseTheSameEndpoints() {
    for (int width : new int[]{8, 18, 47, 90, 200}) {
      int left = 31, thumb = SliderGeometry.thumbWidth(width);
      assertEquals(left, SliderGeometry.thumbX(left, width, 0));
      assertEquals(left + width, SliderGeometry.thumbX(left, width, 1) + thumb);
      assertEquals(left, SliderGeometry.thumbX(left, width, -10));
      assertEquals(left + width, SliderGeometry.thumbX(left, width, 10) + thumb);
      assertEquals(0, SliderGeometry.fraction(left + thumb / 2d, left, width));
      if (width > thumb) assertEquals(1, SliderGeometry.fraction(left + width - thumb / 2d, left, width));
      for (int step = 0; step <= 100; step++) {
        int knob = SliderGeometry.thumbX(left, width, step / 100d);
        assertTrue(knob >= left && knob + thumb <= left + width);
      }
    }
  }

  @Test void exactEntryCheckEndsAtTheOriginalSliderEdgeAndUsesIdenticalHitBounds() {
    for (int width : new int[]{26, 47, 90, 200}) {
      var box = new InlineValueGeometry(31, 42, width);
      assertEquals(31 + width, box.checkX() + box.checkWidth());
      assertEquals(box.checkX() - InlineValueGeometry.GAP, box.x() + box.inputWidth() + 2);
      assertTrue(box.checkContains(31 + width - 1, 59, 0, 200));
      assertFalse(box.checkContains(31 + width, 59, 0, 200));
      assertFalse(box.checkContains(31 + width - 1, 60, 0, 200));
      assertFalse(box.checkContains(31 + width - 1, 43, 44, 200));
    }
  }

  @Test void separatorGapsNeverActivateAdjacentRows() {
    for (int height : new int[]{18, 20}) {
      var rows = new PopupLayout.Rows(4, height, Set.of(1, 3));
      assertEquals(4 * height + 10, rows.contentHeight());
      for (int index = 0; index < 4; index++) {
        assertEquals(index, rows.indexAt(rows.top(index)));
        assertEquals(index, rows.indexAt(rows.top(index) + height - 1));
        if (rows.separators().contains(index))
          for (int y = rows.top(index) - PopupLayout.SEPARATOR_HEIGHT; y < rows.top(index); y++)
            assertEquals(-1, rows.indexAt(y));
      }
      assertEquals(-1, rows.indexAt(-1));
      assertEquals(-1, rows.indexAt(rows.contentHeight()));
    }
  }

  @Test void popupBoundsFitSmallScreensAtEveryEdge() {
    for (int[] screen : new int[][]{{320, 240}, {427, 240}, {640, 420}})
      for (int x : new int[]{0, screen[0] / 2, screen[0]})
        for (int y : new int[]{0, screen[1] / 2, screen[1]})
          for (int height : new int[]{18, 20}) {
            var dropdown = PopupLayout.dropdown(x, y, 500, height, height * 80, screen[0], screen[1]);
            var context = PopupLayout.context(x, y, 500, height * 80, screen[0], screen[1]);
            for (var bounds : new PopupLayout.Bounds[]{dropdown, context}) {
              assertTrue(bounds.x() >= 1 && bounds.y() >= 1);
              assertTrue(bounds.x() + bounds.width() < screen[0]);
              assertTrue(bounds.y() + bounds.height() < screen[1]);
            }
          }
  }

  @Test void dropdownGapFollowsTheActualOwnerButtonHeight() {
    for (int height : new int[]{18, 20}) {
      var bounds = PopupLayout.dropdown(20, 40, 160, height, height * 3, 320, 240);
      assertEquals(40 + height + 2, bounds.y());
      assertEquals(height * 3, bounds.height());
    }
  }

  @Test void tooltipsHaveEqualPaddingAndStayInsideTheBottomEdge() {
    for (int lines : new int[]{1, 2, 6}) {
      var box = TooltipLayout.at(320, 240, 120, lines, 319, 239);
      assertEquals(lines * 11 + 5, box.height());
      assertEquals(8, box.height() - ((lines - 1) * 11 + 8));
      assertTrue(box.x() >= 1 && box.y() >= 1);
      assertTrue(box.x() + box.width() < 320);
      assertTrue(box.y() + box.height() < 240);
    }
  }
}
