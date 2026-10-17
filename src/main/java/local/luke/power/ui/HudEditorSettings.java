package local.luke.power.ui;

import java.util.Set;
import local.luke.power.config.Setting;

/** Routing is presentation-only: saved IDs and independent layouts remain intact. */
public final class HudEditorSettings {
  public static final String ENTRY = "power_hud:hudpositions.hudPositioningSystem";
  private static final Set<String> GROUPS = Set.of("HUD layout", "Combined HUD position",
      "Hotbar position", "Hearts position", "Armor position", "Oxygen position",
      "Overlay message position", "Version label", "Debug (F3) info", "Custom HUD info", "Active tweaks HUD");
  private HudEditorSettings() {}
  public static boolean contains(Setting s) {
    if (s.id.startsWith("activeTweaks.") && s.id.endsWith("Messages")) return false;
    return s.id.equals(ENTRY) || s.id.startsWith("infoHud.")
        || s.id.startsWith("activeTweaks.") && !s.id.endsWith("Messages")
        || s.page.equals("Interface") && GROUPS.contains(s.group);
  }
  public static boolean lettering(Setting s) {
    return s.id.startsWith("logo.logo.") || s.id.startsWith("logo.blocks.") || s.id.equals("logo.presets");
  }
}
