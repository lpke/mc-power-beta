package local.luke.power.config;

/** Persistence scope, including world overrides stored outside the world backend. */
public final class SettingScope {
  private SettingScope() {}

  public static boolean perWorld(Setting setting) {
    return perWorld(setting.id, setting.backend);
  }

  private static boolean perWorld(String id, String backend) {
    return backend.equals("world") || id.startsWith("commands.world.")
        || id.startsWith("commands.worldMode.");
  }

  public static String label(String id, String backend, String group, String label) {
    if (!perWorld(id, backend) || group.equals("World overrides") || group.toLowerCase(java.util.Locale.ROOT).contains("this world")
        || label.toLowerCase(java.util.Locale.ROOT).contains("this world")) return label;
    return label + " (this world)";
  }

  public static String note(Setting setting) {
    if (perWorld(setting)) return "Saved separately for each singleplayer world.";
    return "";
  }
}
