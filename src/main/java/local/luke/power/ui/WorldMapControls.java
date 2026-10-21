package local.luke.power.ui;

import java.util.*;
import java.util.function.ToIntFunction;

/** Bottom map controls keep their measured drawing and pointer bounds together. */
record WorldMapControls(List<Button> buttons, Box scale) {
  enum Action { LIGHT, MOBS, SLIME, CHUNKS, PATH, WAYPOINTS, SETTINGS, LAYER, DIMENSION, CENTER, GO_TO, CLOSE }
  record Box(int x, int y, int width, int height) {
    boolean contains(int px, int py) { return px >= x && py >= y && px < x + width && py < y + height; }
    boolean intersects(Box other) { return x < other.x + other.width && x + width > other.x && y < other.y + other.height && y + height > other.y; }
    Box translated(int dx, int dy) { return new Box(x + dx, y + dy, width, height); }
  }
  record Button(Action action, String label, Box bounds) {}

  static WorldMapControls at(int left, int width, int height, String[] labels, ToIntFunction<String> measure) {
    if (labels.length != Action.values().length) throw new IllegalArgumentException("Missing map control labels");
    labels = labels.clone();
    int[] sizes = Arrays.stream(labels).mapToInt(label -> Math.max(42, measure.applyAsInt(label) + 12)).toArray();
    int available = Math.max(1, width - 16);
    fit(sizes, 5, 9, available); fit(sizes, 9, 12, available);
    int bottomWidth = total(sizes, 5, 9), rightWidth = total(sizes, 9, 12);
    int rightY = height - 24, leftY = rightY - (bottomWidth + rightWidth + 8 <= available ? 0 : 22);
    var toggleRows = new ArrayList<int[]>();
    int start = 0, used = 0;
    for (int i = 0; i < 5; i++) {
      if (i > start && used + 4 + sizes[i] > available) {
        toggleRows.add(new int[]{start, i}); start = i; used = 0;
      }
      used += (i == start ? 0 : 4) + sizes[i];
    }
    toggleRows.add(new int[]{start, 5});
    var buttons = new ArrayList<Button>();
    for (int i = 0; i < toggleRows.size(); i++) {
      int[] range = toggleRows.get(i);
      row(buttons, labels, sizes, range[0], range[1], left + 8, leftY - (toggleRows.size() - i) * 22);
    }
    row(buttons, labels, sizes, 5, 9, left + 8, leftY);
    row(buttons, labels, sizes, 9, 12, left + width - 8 - rightWidth, rightY);
    Box scale = new Box(left + width - 124, rightY - 36, 116, 20);
    for (Button button : buttons) if (button.bounds.intersects(scale)) {
      scale = new Box(scale.x, leftY - toggleRows.size() * 22 - 24, scale.width, scale.height); break;
    }
    return new WorldMapControls(List.copyOf(buttons), scale);
  }

  private static void fit(int[] widths, int start, int end, int available) {
    int total = total(widths, start, end);
    if (total <= available) return;
    int room = Math.max(end - start, available - (end - start - 1) * 4);
    int original = total - (end - start - 1) * 4, used = 0;
    for (int i = start; i < end; i++) {
      int width = i == end - 1 ? room - used : Math.max(1, widths[i] * room / original);
      widths[i] = width; used += width;
    }
  }
  private static int total(int[] widths, int start, int end) {
    int total = (end - start - 1) * 4;
    for (int i = start; i < end; i++) total += widths[i];
    return total;
  }
  private static void row(List<Button> buttons, String[] labels, int[] sizes, int start, int end, int x, int y) {
    for (int i = start; i < end; i++) {
      buttons.add(new Button(Action.values()[i], labels[i], new Box(x, y, sizes[i], 18)));
      x += sizes[i] + 4;
    }
  }
  Button at(int x, int y) { return buttons.stream().filter(button -> button.bounds.contains(x, y)).findFirst().orElse(null); }
  Button button(Action action) { return buttons.stream().filter(button -> button.action == action).findFirst().orElseThrow(); }
}
