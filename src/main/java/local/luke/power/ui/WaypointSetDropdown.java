package local.luke.power.ui;

import java.util.List;

/** Waypoint set selectors use the HUD editor's border, focus, hover and selection colors. */
final class WaypointSetDropdown {
  private WaypointSetDropdown() {}

  static void button(UiScreen screen, String label, int x, int y, int width,
      int mx, int my, boolean enabled, boolean open) {
    screen.button("", x, y, width, 18, mx, my, enabled);
    screen.text(screen.fit(label, width - 24), x + 5, y + 5, enabled ? 0xe0e0e0 : 0xa0a0a0);
    screen.chevron(x + width - 13, y + 7, open, 0xffdddddd);
  }

  static void render(UiScreen screen, List<String> labels, int selected, int focus, int scroll,
      int x, int y, int width, int rows, int mx, int my) {
    screen.rectangle(x - 2, y - 2, x + width + 2, y + rows * 20 + 2, 0xff777777);
    screen.rectangle(x - 1, y - 1, x + width + 1, y + rows * 20 + 1, 0xff181818);
    for (int i = 0; i < rows && i + scroll < labels.size(); i++) {
      int index = i + scroll, top = y + i * 20;
      String label = labels.get(index);
      boolean hover = screen.inside(mx, my, x, top, width, 20), active = index == selected;
      if (hover || index == focus) screen.rectangle(x, top, x + width - 1, top + 19, 0xff3c3c3c);
      if (active) screen.rectangle(x + 2, top + 3, x + 4, top + 16, 0xff268bff);
      screen.text(screen.fit(label, width - 17), x + 9, top + 6, hover || active ? 0xffffff : 0xcccccc);
    }
  }
}
