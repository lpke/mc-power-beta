package local.luke.power.death;

import local.luke.power.config.Catalog;
import local.luke.power.storage.PowerConfig;

public final class DeathConfig {
  private static DeathSettings current = PowerConfig.read("death", DeathSettings.class);
  private DeathConfig() {}
  public static DeathSettings current() { return current; }
  public static DeathSettings copy() { return Catalog.JSON.fromJson(Catalog.JSON.toJson(current), DeathSettings.class); }
  public static void preview(DeathSettings next) { current = Catalog.JSON.fromJson(Catalog.JSON.toJson(next), DeathSettings.class); }
  public static void save(DeathSettings next) throws Exception { PowerConfig.save("death", next); preview(next); }
}
