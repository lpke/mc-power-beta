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
    boolean highlighted = enabled && (open || screen.inside(mx, my, x, y, width, height));
    int color = !enabled ? 0xa0a0a0 : highlighted ? PopupStyle.HOVER_TEXT : 0xe0e0e0;
    centeredLabel(screen, label, x, y + (height - 8) / 2, width, 19, color);
    screen.chevron(x + width - 11, y + (height - 4) / 2, open, 0xff000000 | color);
  }

  static void render(UiScreen screen, List<String> labels, int selected, int focus, int scroll,
      int x, int y, int width, int rows, int mx, int my) {
    render(screen, labels, selected, focus, scroll, x, y, width, 20, rows, mx, my);
  }

  static void render(UiScreen screen, List<String> labels, int selected, int focus, int scroll,
      int x, int y, int width, int rowHeight, int rows, int mx, int my) {
    PopupStyle.background(screen, x, y, width, rows * rowHeight, true);
    for (int i = 0; i < rows && i + scroll < labels.size(); i++) {
      int index = i + scroll, top = y + i * rowHeight;
      row(screen, labels.get(index), x, top, width, rowHeight, mx, my, index == selected, index == focus, 0);
    }
  }
  static void row(UiScreen screen, String label, int x, int top, int width, int mx, int my,
      boolean selected, boolean focused) {
    row(screen, label, x, top, width, mx, my, selected, focused, 0);
  }

  static void row(UiScreen screen, String label, int x, int top, int width, int mx, int my,
      boolean selected, boolean focused, int trailingWidth) {
    row(screen, label, x, top, width, 20, mx, my, selected, focused, trailingWidth);
  }

  static void row(UiScreen screen, String label, int x, int top, int width, int height, int mx, int my,
      boolean selected, boolean focused, int trailingWidth) {
    PopupStyle.row(screen, label, x, top, width, height, mx, my, true, selected, focused, trailingWidth);
  }

  private static void centeredLabel(UiScreen screen, String label, int x, int y, int width,
      int padding, int colour) {
    int available = width - 2 * padding;
    if (available <= 0) return;
    String fitted = screen.fit(label, available);
    int textWidth = screen.textWidth(fitted);
    if (textWidth > available) return;
    screen.text(fitted, x + width / 2 - textWidth / 2, y, colour);
  }
}
