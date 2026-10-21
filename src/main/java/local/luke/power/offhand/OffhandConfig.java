package local.luke.power.offhand;

import local.luke.power.config.Catalog;
import local.luke.power.storage.PowerConfig;

public final class OffhandConfig {
  private static OffhandSettings current = PowerConfig.read("offhand", OffhandSettings.class);
  private OffhandConfig() {}
  public static OffhandSettings current() { return current; }
  public static OffhandSettings copy() { return Catalog.JSON.fromJson(Catalog.JSON.toJson(current), OffhandSettings.class); }
  public static void preview(OffhandSettings value) { current = Catalog.JSON.fromJson(Catalog.JSON.toJson(value), OffhandSettings.class); }
  public static void save(OffhandSettings value) throws Exception { PowerConfig.save("offhand", value); preview(value); }
}
