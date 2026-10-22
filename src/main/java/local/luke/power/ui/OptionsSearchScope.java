package local.luke.power.ui;

import local.luke.power.config.Setting;

/** Search uses the same destination that opening a result will visit. */
final class OptionsSearchScope {
  private static final java.util.Map<String, Boolean> choices = new java.util.HashMap<>();
  private OptionsSearchScope() {}
  static boolean thisTab(String page) { return choices.getOrDefault(page, !page.equals("General")); }
  static void toggle(String page) { choices.put(page, !thisTab(page)); }
  static String page(Setting setting, boolean mapPreview) {
    if (mapPreview && HudEditorSettings.mapAppearance(setting.id)) return "Map";
    return HudEditorSettings.contains(setting) ? "General" : setting.page;
  }
  static String group(Setting setting, int hudMode, boolean mapPreview) {
    if (mapPreview && HudEditorSettings.mapAppearance(setting.id)) return "Map / Minimap appearance";
    if (HudEditorSettings.contains(setting) && !setting.id.equals(HudEditorSettings.ENTRY))
      return "General / Interface / HUD editor / " + HudEditorSettings.tabLabel(HudEditorSettings.tab(setting, hudMode));
    return setting.group.isEmpty() ? setting.page : setting.page + " / " + setting.group;
  }
}
