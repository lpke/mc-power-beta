package local.luke.power.ui;

import java.util.List;

/** Waypoint set selectors use the HUD editor's border, focus, hover and selection colors. */
final class WaypointSetDropdown {
  private WaypointSetDropdown() {}

  static void button(UiScreen screen, String label, int x, int y, int width,
      int mx, int my, boolean enabled, boolean open) {
    button(screen, label, x, y, width, 18, mx, my, enabled, open);
  }

  static void button(UiScreen screen, String label, int x, int y, int width, int height,
      int mx, int my, boolean enabled, boolean open) {
    screen.button("", x, y, width, height, mx, my, enabled);
    centeredLabel(screen, label, x, y + (height - 8) / 2, width, 6, 19, enabled ? 0xe0e0e0 : 0xa0a0a0);
    screen.chevron(x + width - 11, y + (height - 4) / 2, open, 0xffdddddd);
  }

  static void render(UiScreen screen, List<String> labels, int selected, int focus, int scroll,
      int x, int y, int width, int rows, int mx, int my) {
    screen.rectangle(x - 2, y - 2, x + width + 2, y + rows * 20 + 2, 0xff777777);
    screen.rectangle(x - 1, y - 1, x + width + 1, y + rows * 20 + 1, 0xff181818);
    for (int i = 0; i < rows && i + scroll < labels.size(); i++) {
      int index = i + scroll, top = y + i * 20;
      row(screen, labels.get(index), x, top, width, mx, my, index == selected, index == focus);
    }
  }
  static void row(UiScreen screen, String label, int x, int top, int width, int mx, int my,
      boolean selected, boolean focused) {
    row(screen, label, x, top, width, mx, my, selected, focused, 0);
  }

  static void row(UiScreen screen, String label, int x, int top, int width, int mx, int my,
      boolean selected, boolean focused, int trailingWidth) {
    boolean hover = screen.inside(mx, my, x, top, width, 20);
    if (hover || focused) screen.rectangle(x, top, x + width, top + 20, 0xff3c3c3c);
    if (selected) screen.rectangle(x + 2, top + 4, x + 4, top + 16, 0xff268bff);
    centeredLabel(screen, label, x, top + 6, width, 9, 9 + trailingWidth, hover || selected ? 0xffffff : 0xcccccc);
  }

  private static void centeredLabel(UiScreen screen, String label, int x, int y, int width,
      int leftPadding, int rightPadding, int colour) {
    int left = x + leftPadding, right = x + width - rightPadding;
    if (right <= left) return;
    String fitted = screen.fit(label, right - left);
    int textWidth = screen.textWidth(fitted);
    if (textWidth > right - left) return;
    int centered = x + (width - textWidth) / 2;
    screen.text(fitted, Math.max(left, Math.min(centered, right - textWidth)), y, colour);
  }
}
