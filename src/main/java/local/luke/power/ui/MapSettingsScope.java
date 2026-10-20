package local.luke.power.ui;

import local.luke.power.config.Setting;

/** One boundary for map search, navigation and resets; hidden backend dependencies stay in the session. */
final class MapSettingsScope {
  private MapSettingsScope() {}
  static boolean contains(Setting setting) {
    return setting.id.startsWith("maps.") || setting.kind == Setting.Kind.KEY && setting.group.equals("Maps");
  }
  static int panelWidth(int width, int height) {
    return width >= 800 && height >= 300 ? Math.min(430, width * 42 / 100) : width;
  }
}
