package local.luke.power.config;

import local.luke.power.permissions.CommandPermissions;

public final class SettingAccess {
  private SettingAccess() {}

  public static boolean visible(ConfigSession session, Setting setting) {
    if (setting.id.startsWith("logo.logo.") || setting.id.startsWith("logo.blocks.")) {
      int style = session.settings().stream().filter(s -> s.id.equals("logo.style"))
          .mapToInt(s -> s.value.getAsInt()).findFirst().orElse(0);
      if (style != 0) return false;
    }
    if (setting.id.startsWith("power_hud:hudpositions.")) {
      int mode = session.settings().stream().filter(s -> s.id.equals("power_hud:hudpositions.hudPositioningSystem"))
          .mapToInt(s -> s.value.getAsInt()).findFirst().orElse(1);
      if (setting.id.contains(".SIMPLE_HUD_POSITION_CONFIG.")) return mode == 1;
      if (setting.id.contains(".ADVANCED_HUD_POSITIONS_CONFIG.")) return mode == 2;
    }
    var rule = owner(setting);
    if (rule == null) return true;
    boolean inWorld = session.settings().stream().anyMatch(s -> s.id.startsWith("rules.world."));
    if (!inWorld) return !CommandPermissions.context().multiplayer();
    if (setting.id.contains("power_creative.picker") || setting.id.contains("power_creative.modifier"))
      return enabled(session, local.luke.power.rules.GameRule.ALLOW_CREATIVE_MODE) || enabled(session, local.luke.power.rules.GameRule.ALLOW_SPECTATOR_MODE);
    if (setting.backend.equals("creative") && setting.group.equals("Flight"))
      return enabled(session, local.luke.power.rules.GameRule.ALLOW_FLIGHT)
          || enabled(session, local.luke.power.rules.GameRule.ALLOW_CREATIVE_MODE);
    return enabled(session, rule);
  }
  private static boolean enabled(ConfigSession session, local.luke.power.rules.GameRule rule) {
    return session.settings().stream().filter(s -> s.id.equals("rules.world." + rule.id())).anyMatch(s -> s.value.getAsBoolean());
  }
  public static local.luke.power.rules.GameRule owner(Setting setting) {
    String id = setting.id;
    if (id.startsWith("rules.")) return null;
    if (id.equals("keys.powerbeta.sprint") || id.startsWith("sprint.")
        && !java.util.Set.of("sprint.dynamicFov", "sprint.creativeFov", "sprint.spectatorFov", "sprint.freecamFov", "sprint.fovStrength").contains(id))
      return local.luke.power.rules.GameRule.ALLOW_SPRINTING;
    if (id.startsWith("worldedit.")) return local.luke.power.rules.GameRule.ALLOW_WORLD_EDITING;
    if (id.equals("visual.spectatorFullBright")
        || id.startsWith("creative.spectator") || id.equals("creative.showSpectatorSpeed")) return local.luke.power.rules.GameRule.ALLOW_SPECTATOR_MODE;
    // Reach is also used by container previews; the sprint binding is shared across modes.
    if (java.util.Set.of("creative.blockReach", "creative.entityReach", "keys.power_creative.sprint").contains(id)) return null;
    if (setting.backend.equals("creative") || id.contains("power_creative.") || id.equals("gameplay.leavesDropSaplingsInCreative"))
      return local.luke.power.rules.GameRule.ALLOW_CREATIVE_MODE;
    return null;
  }
  /** The gate changes presentation, never the stored preference beneath it. */
  public static String lockedValue(Setting setting) {
    if (setting.id.equals("audio.musicMode") || setting.id.equals("audio.customMusic")) return "Using preset";
    return setting.kind == Setting.Kind.BOOLEAN ? "Off" : "Disabled";
  }

  public static String reason(ConfigSession session, Setting setting) {
    if (setting.id.startsWith("power_hud:hudpositions.")
        && !setting.id.endsWith("hudPositioningSystem")) {
      int mode = session.settings().stream().filter(s -> s.id.equals("power_hud:hudpositions.hudPositioningSystem"))
          .mapToInt(s -> s.value.getAsInt()).findFirst().orElse(1);
      if (mode == 0) return "Choose Combined or Separate in HUD positioning to customize the HUD.";
      if (setting.id.contains(".SIMPLE_HUD_POSITION_CONFIG.") && mode != 1)
        return "Choose Combined in HUD positioning to move the whole HUD together.";
      if (setting.id.contains(".ADVANCED_HUD_POSITIONS_CONFIG.") && mode != 2)
        return "Choose Separate in HUD positioning to move this element.";
    }
    if (setting.id.equals("audio.musicMode") || setting.id.equals("audio.customMusic")) {
      Setting preset = session.settings().stream().filter(s -> s.id.equals("audio.preset")).findFirst().orElse(null);
      if (preset != null && !preset.value.getAsString().isEmpty()) return "Using the selected World music preset. Choose None to use these settings.";
    }
    if (visible(session, setting)) return "";
    var rule = owner(setting);
    return rule == null ? "" : "Enable " + rule.label() + " in Game rules.";
  }
}
