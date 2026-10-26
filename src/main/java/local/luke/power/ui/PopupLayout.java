package local.luke.power.ui;

import java.util.Set;

/** Popup row positions include one-pixel separators in drawing, scrolling and hit tests. */
final class PopupLayout {
  static final int SEPARATOR_HEIGHT = 1;
  private PopupLayout() {}

  record Rows(int count, int height, Set<Integer> separators) {
    Rows {
      if (count < 1 || height < 8) throw new IllegalArgumentException("Invalid popup rows");
      separators = Set.copyOf(separators);
      if (separators.stream().anyMatch(i -> i < 1 || i >= count))
        throw new IllegalArgumentException("Separators must precede an existing row after the first");
    }
    int top(int index) {
      return index * height + SEPARATOR_HEIGHT * (int)separators.stream().filter(i -> i <= index).count();
    }
    int contentHeight() { return top(count - 1) + height; }
    int indexAt(int position) {
      if (position < 0 || position >= contentHeight()) return -1;
      for (int i = 0; i < count; i++) if (position >= top(i) && position < top(i) + height) return i;
      return -1;
    }
  }

  record Bounds(int x, int y, int width, int height) {}

  static Bounds dropdown(int left, int top, int span, int anchorHeight, int contentHeight,
      int screenWidth, int screenHeight) {
    int width = Math.max(1, Math.min(Math.max(span, 150), screenWidth - 12));
    int x = Math.max(6, Math.min(left, screenWidth - width - 6));
    int belowTop = top + anchorHeight + 2;
    int below = Math.max(0, screenHeight - belowTop - 6), above = Math.max(0, top - 8);
    int height = Math.min(contentHeight, Math.max(1, Math.max(below, above)));
    int y = below >= height ? belowTop : Math.max(6, top - height - 2);
    return new Bounds(x, y, width, height);
  }

  static Bounds context(int left, int top, int span, int contentHeight, int screenWidth, int screenHeight) {
    int width = Math.max(1, Math.min(span, screenWidth - 12));
    int height = Math.max(1, Math.min(contentHeight, screenHeight - 12));
    return new Bounds(Math.max(6, Math.min(left, screenWidth - width - 6)),
        Math.max(6, Math.min(top, screenHeight - height - 6)), width, height);
  }
}
