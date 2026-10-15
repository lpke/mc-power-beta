package local.luke.power.config;

/** Explicit world data; all other settings are global. */
public final class SettingScope {
  private SettingScope() {}

  public static boolean perWorld(Setting setting) {
    return perWorld(setting.id, setting.backend);
  }

  private static boolean perWorld(String id, String backend) {
    return backend.equals("world");
  }

  public static String label(String id, String backend, String group, String label) {
    if (id.startsWith("rules.") || !perWorld(id, backend) || group.equals("World overrides") || group.toLowerCase(java.util.Locale.ROOT).contains("this world")
        || label.toLowerCase(java.util.Locale.ROOT).contains("this world")) return label;
    return label + " (this world)";
  }

  public static String note(Setting setting) {
    if (perWorld(setting)) return "Saved separately for each singleplayer world.";
    return "";
  }
}
