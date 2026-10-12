package local.luke.power.gameplay;

import local.luke.power.config.Catalog;
import local.luke.power.storage.PowerConfig;

public final class GameplayConfig {
  private static GameplaySettings current = load();
  private GameplayConfig() {}
  private static GameplaySettings load() {
    GameplaySettings settings = PowerConfig.read("gameplay", GameplaySettings.class);
    settings.validate();
    return settings;
  }
  public static GameplaySettings current() { return current; }
  public static GameplaySettings copy() { return Catalog.JSON.fromJson(Catalog.JSON.toJson(current), GameplaySettings.class); }
  public static void preview(GameplaySettings settings) {
    settings.validate();
    current = Catalog.JSON.fromJson(Catalog.JSON.toJson(settings), GameplaySettings.class);
  }
  public static void save(GameplaySettings settings) throws java.io.IOException {
    settings.validate();
    PowerConfig.save("gameplay", settings);
    preview(settings);
  }
}
