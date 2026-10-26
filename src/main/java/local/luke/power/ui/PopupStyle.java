package local.luke.power.ui;

/** Shared choice lists and action menus. Only choice lists draw an outside border. */
public final class PopupStyle {
  public static final int BACKGROUND = 0xf0202020, BORDER = 0xff777777,
      HOVER = 0xff444444, SELECTED = 0xff3c3c3c, ACTIVE = 0xff268bff,
      TEXT = 0xcccccc, BRIGHT_TEXT = 0xffffff, DISABLED_TEXT = 0x777777, PADDING = 9;

  private PopupStyle() {}

  public static void background(UiScreen screen, int x, int y, int width, int height, boolean bordered) {
    if (bordered) screen.rectangle(x - 1, y - 1, x + width + 1, y + height + 1, BORDER);
    screen.rectangle(x, y, x + width, y + height, BACKGROUND);
  }

  public static void separator(UiScreen screen, int x, int y, int width) {
    screen.rectangle(x, y + 2, x + width, y + 3, BORDER);
  }

  public static void row(UiScreen screen, String label, int x, int top, int width, int height,
      int mx, int my, boolean enabled, boolean selected, boolean focused, int trailingWidth) {
    boolean hover = enabled && screen.inside(mx, my, x, top, width, height);
    if (selected || hover || enabled && focused)
      screen.rectangle(x, top, x + width, top + height, selected ? SELECTED : HOVER);
    if (selected) screen.rectangle(x + 2, top + 4, x + 4, top + height - 4, ACTIVE);
    int available = width - 2 * PADDING - trailingWidth;
    if (available <= 0) return;
    String fitted = screen.fit(label, available);
    if (screen.textWidth(fitted) <= available)
      screen.text(fitted, x + PADDING, top + (height - 8) / 2,
          !enabled ? DISABLED_TEXT : hover || selected || focused ? BRIGHT_TEXT : TEXT);
  }
}
