package local.luke.power.ui;

/** Horizontal content tabs share an open selected edge and a continuous baseline. */
public final class ConnectedTabs {
  private ConnectedTabs() {}

  public static void baseline(UiScreen screen, int left, int end, int y, int selectedX, int selectedWidth) {
    baseline(screen, left, end, y, 18, selectedX, selectedWidth);
  }

  public static void baseline(UiScreen screen, int left, int end, int y, int height, int selectedX, int selectedWidth) {
    int bottom = y + height - 1;
    if (selectedX > left) screen.rectangle(left, bottom, selectedX, bottom + 1, 0xff777777);
    if (selectedX + selectedWidth < end)
      screen.rectangle(selectedX + selectedWidth, bottom, end, bottom + 1, 0xff777777);
  }

  public static void tab(UiScreen screen, String label, int x, int y, int width, int mx, int my, boolean selected) {
    tab(screen, label, x, y, width, 18, mx, my, selected);
  }

  public static void tab(UiScreen screen, String label, int x, int y, int width, int height, int mx, int my, boolean selected) {
    boolean hovered = screen.inside(mx, my, x, y, width, height);
    int right = x + width, bottom = y + height - 1;
    if (!selected) screen.rectangle(x, y + 1, right, bottom, 0x99000000);
    int top = selected ? y : y + 1, edge = selected ? 0xffbbbbbb : 0xff4a4a4a;
    int edgeBottom = selected ? bottom + 1 : bottom;
    screen.rectangle(x, top, right, top + 1, edge);
    screen.rectangle(x, top, x + 1, edgeBottom, edge);
    screen.rectangle(right - 1, top, right, edgeBottom, edge);
    screen.text(screen.fit(label, width - 12), x + 6, y + (height - 8) / 2,
        selected ? 0xffffff : hovered ? 0xcccccc : 0x999999);
  }
}
