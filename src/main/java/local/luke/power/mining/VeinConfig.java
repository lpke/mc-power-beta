package local.luke.power.mining;

import local.luke.power.config.Catalog;
import local.luke.power.storage.PowerConfig;

public final class VeinConfig {
  private static VeinSettings current = load();
  private VeinConfig() {}
  private static VeinSettings load() {
    var settings = PowerConfig.read("veinMining", VeinSettings.class); settings.validate(); return settings;
  }
  public static VeinSettings current() { return current; }
  public static VeinSettings copy() { return Catalog.JSON.fromJson(Catalog.JSON.toJson(current), VeinSettings.class); }
  public static void save(VeinSettings settings) throws java.io.IOException {
    settings.validate(); PowerConfig.save("veinMining", settings);
    current = Catalog.JSON.fromJson(Catalog.JSON.toJson(settings), VeinSettings.class);
    VeinMining.reset();
  }
}
