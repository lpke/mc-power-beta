package local.luke.power.ui;

import java.util.Set;
import local.luke.power.config.Setting;

/** Routing is presentation-only: saved IDs and independent layouts remain intact. */
public final class HudEditorSettings {
  public static final String ENTRY = "power_hud:hudpositions.hudPositioningSystem";
  static final String HUD = "power_hud:config.";
  static final String ITEM_NAMES = HUD + "enableHotbarItemSelectionTooltips";
  static final String ITEM_FADE = HUD + "hotbarItemSelectionFadeTime";
  static final String TOAST = "power_controls:userinterface.hideAchievementToast";
  static final String COLOURS = "interface.colours";
  private static final Set<String> GROUPS = Set.of("HUD layout", "Combined HUD position",
      "Hotbar position", "Hearts position", "Armor position", "Oxygen position",
      "Overlay message position", "Version label", "Debug (F3) info", "Custom HUD info", "Active tweaks HUD", "General HUD", "Chat");
  private HudEditorSettings() {}
  public static boolean contains(Setting s) {
    if (s.id.startsWith("activeTweaks.") && s.id.endsWith("Messages")) return false;
    return s.id.equals(COLOURS) || s.id.equals(TOAST) || s.id.equals(ENTRY) || s.id.startsWith("infoHud.")
        || s.id.startsWith("activeTweaks.") && !s.id.endsWith("Messages")
        || s.page.equals("Interface") && GROUPS.contains(s.group);
  }
  static String view(Setting s) {
    if (s.id.equals(HUD + "disableCrosshair")) return "layout";
    if (s.id.equals(HUD + "drawXboxXAndYButtons")) return "layout";
    if (s.id.equals(TOAST)) return "layout";
    if (s.group.equals("Chat")) return "chat";
    return "";
  }
  static boolean itemNames(Setting s) { return s.id.equals(ITEM_NAMES) || s.id.equals(ITEM_FADE); }
  private static boolean internalLogo(Setting s) { return s.id.equals("logo.selectedPreset") || s.id.equals("logo.retroBase"); }
  public static boolean internal(Setting s) { return internalLogo(s) || s.id.equals(COLOURS); }
  public static boolean lettering(Setting s) {
    return s.id.startsWith("logo.logo.") || s.id.startsWith("logo.blocks.") || s.id.equals("logo.presets") || s.id.equals("logo.swatches") || internalLogo(s);
  }
}
