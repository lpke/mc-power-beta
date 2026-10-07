package local.luke.power.config;

import local.luke.power.permissions.CommandPermissions;

public final class SettingAccess {
  private SettingAccess() {}

  public static boolean visible(ConfigSession session, Setting setting) {
    return !cheat(setting)
        || session.settings().stream().anyMatch(s -> s.id.equals("world.cheats") && s.value.getAsBoolean());
  }

  public static boolean cheat(Setting s) {
    if (s.id.equals("world.daylightCycle") || s.id.equals("world.weatherCycle")) return true;
    // These two preferences also control freecam, which is not cheats-gated.
    if (s.id.equals("creative.sprintToggle") || s.id.equals("creative.sprintMultiplier"))
      return false;
    if (s.backend.equals("creative") || s.id.startsWith("worldedit.")) return true;
    if (s.id.startsWith("commands.")) {
      String name = s.id.substring(s.id.lastIndexOf('.') + 1);
      return CommandPermissions.COMMANDS.stream()
          .filter(c -> c.name().equals(name))
          .findFirst()
          .map(CommandPermissions.Command::cheat)
          .orElse(true);
    }
    return s.kind == Setting.Kind.KEY
        && s.id.contains("power_creative.")
        && !s.id.endsWith(".sprint");
  }

  /** The gate changes presentation, never the stored preference beneath it. */
  public static String lockedValue(Setting setting) {
    if (setting.id.equals("audio.musicMode") || setting.id.equals("audio.customMusic")) return "Using preset";
    return setting.kind == Setting.Kind.BOOLEAN ? "Off" : "Disabled";
  }

  public static String reason(ConfigSession session, Setting setting) {
    if (setting.id.equals("audio.musicMode") || setting.id.equals("audio.customMusic")) {
      Setting preset = session.settings().stream().filter(s -> s.id.equals("audio.preset")).findFirst().orElse(null);
      if (preset != null && !preset.value.getAsString().isEmpty()) return "Using the selected World music preset. Choose None to use these settings.";
    }
    if (!cheat(setting)) return "";
    Setting master =
        session.settings().stream()
            .filter(s -> s.id.equals("world.cheats"))
            .findFirst()
            .orElse(null);
    if (master == null) return "Open a singleplayer world to change cheat settings.";
    return master.value.getAsBoolean()
        ? ""
        : "Disabled because Cheats enabled is off. Change it in General > Game.";
  }
}
