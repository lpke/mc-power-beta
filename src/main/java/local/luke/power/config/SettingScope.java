package local.luke.power.config;

/** Persistence scope, including world overrides stored outside the world backend. */
public final class SettingScope {
  private SettingScope() {}

  public static boolean perWorld(Setting setting) {
    return setting.backend.equals("world") || setting.id.startsWith("commands.world.")
        || setting.id.startsWith("commands.worldMode.");
  }

  public static String note(Setting setting) {
    if (perWorld(setting)) return "Saved separately for each singleplayer world.";
    if (setting.kind == Setting.Kind.KEY) return "Global binding. Shared across worlds.";
    return "Global setting. Shared across worlds in this instance.";
  }
}
