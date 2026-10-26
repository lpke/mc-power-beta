package local.luke.power.ui;

/** The selected tab opens onto the ordinary content area; inactive tabs stay dark. */
final class MusicTabs {
  private MusicTabs() {}
  static void render(UiScreen screen, MusicFilters.Layout layout, String selected, int end, int mx, int my) {
    var active = layout.tabs().stream().filter(tab -> tab.id().equals(selected)).findFirst().orElseThrow();
    int bottom = active.y() + 17;
    screen.rectangle(active.x() + active.width(), bottom, end, bottom + 1, 0xff777777);
    int left = layout.tabs().get(0).x();
    if (active.x() > left) screen.rectangle(left, bottom, active.x(), bottom + 1, 0xff777777);
    for (var tab : layout.tabs()) {
      boolean chosen = tab.id().equals(selected), hover = screen.inside(mx, my, tab.x(), tab.y(), tab.width(), 18);
      int x = tab.x(), y = tab.y(), right = x + tab.width();
      if (!chosen) screen.rectangle(x, y + 1, right, bottom, 0x99000000);
      int top = chosen ? y : y + 1, color = chosen ? 0xffbbbbbb : 0xff4a4a4a;
      int edgeBottom = chosen ? bottom + 1 : bottom;
      screen.rectangle(x, top, right, top + 1, color);
      screen.rectangle(x, top, x + 1, edgeBottom, color);
      screen.rectangle(right - 1, top, right, edgeBottom, color);
      screen.text(tab.label(), x + 6, y + 5, chosen ? 0xffffff : hover ? 0xcccccc : 0x999999);
    }
  }
}
