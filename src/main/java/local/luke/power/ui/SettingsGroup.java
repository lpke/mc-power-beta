package local.luke.power.ui;

import java.util.Set;

/** Shared Options and editor headings. Opening a group never changes a setting. */
final class SettingsGroup {
  static final int HEIGHT = 24;
  private SettingsGroup() {}
  static void draw(UiScreen screen, String label, boolean collapsed, int left, int right, int y) {
    screen.text((collapsed ? "> " : "v ") + label, left, y + 8, 0xffffff);
    screen.rectangle(left, y + 21, right, y + 22, 0x50555555);
  }
  static void toggle(Set<String> collapsed, String key) {
    if (!collapsed.add(key)) collapsed.remove(key);
  }
}
