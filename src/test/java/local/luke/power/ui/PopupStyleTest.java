package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PopupStyleTest {
  record Box(int left, int top, int right, int bottom, int colour) {}
  record Label(String text, int x, int y, int colour) {}
  static final class Host extends UiScreen {
    final List<Box> boxes = new ArrayList<>();
    final List<Label> labels = new ArrayList<>();
    int clicks;
    @Override void rectangle(int left, int top, int right, int bottom, int colour) {
      boxes.add(new Box(left, top, right, bottom, colour));
    }
    @Override protected void text(String text, int x, int y, int colour) { labels.add(new Label(text, x, y, colour)); }
    @Override int textWidth(String text) { return text.length() * 6; }
    @Override protected String fit(String text, int width) { return text.substring(0, Math.min(text.length(), Math.max(0, width / 6))); }
    @Override protected void click() { clicks++; }
  }

  @Test void dropdownHoverMeetsItsBorderAndContextMenusHaveNoBorder() {
    var screen = new Host();
    PopupStyle.background(screen, 10, 20, 150, 36, true);
    PopupStyle.row(screen, "First", 10, 20, 150, 18, 10, 20, true, false, false, 0);
    assertEquals(new Box(9, 19, 161, 57, PopupStyle.BORDER), screen.boxes.get(0));
    assertEquals(new Box(10, 20, 160, 38, PopupStyle.HOVER), screen.boxes.get(2));
    screen.boxes.clear();
    PopupStyle.background(screen, 10, 20, 150, 36, false);
    assertEquals(List.of(new Box(10, 20, 160, 56, PopupStyle.BACKGROUND)), screen.boxes);
  }

  @Test void selectedRowsUseTheSharedBlueIndicatorAndCenterTextAtBothControlHeights() {
    for (int height : new int[]{18, 20}) {
      var screen = new Host();
      PopupStyle.row(screen, "Current", 10, 20, 150, height, -1, -1, true, true, false, 0);
      assertEquals(new Box(10, 20, 160, 20 + height, PopupStyle.SELECTED), screen.boxes.get(0));
      assertEquals(new Box(12, 24, 14, 20 + height - 4, PopupStyle.ACTIVE), screen.boxes.get(1));
      assertEquals(new Label("Current", 19, 20 + (height - 8) / 2, PopupStyle.BRIGHT_TEXT), screen.labels.get(0));
    }
  }

  @Test void disabledActionsHaveNoHoverFill() {
    var screen = new Host();
    PopupStyle.row(screen, "Unavailable", 10, 20, 150, 18, 20, 25, false, false, false, 0);
    assertTrue(screen.boxes.isEmpty());
    assertEquals(PopupStyle.DISABLED_TEXT, screen.labels.get(0).colour());
  }
}
