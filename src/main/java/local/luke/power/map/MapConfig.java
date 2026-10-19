package local.luke.power.map;

import local.luke.power.PowerBeta;
import local.luke.power.config.Catalog;
import local.luke.power.storage.PowerConfig;

public final class MapConfig {
  private static MapSettings current = load();

  private MapConfig() {}

  private static MapSettings load() {
    try {
      MapSettings value = PowerConfig.read("maps", MapSettings.class);
      value.validate();
      return value;
    } catch (Exception e) {
      PowerBeta.LOG.error("Could not load map preferences", e);
      return new MapSettings();
    }
  }

  public static MapSettings current() {
    return current;
  }

  public static MapSettings copy() {
    return Catalog.JSON.fromJson(Catalog.JSON.toJson(current), MapSettings.class);
  }

  public static void preview(MapSettings value) {
    value.validate();
    current = Catalog.JSON.fromJson(Catalog.JSON.toJson(value), MapSettings.class);
  }

  public static void save(MapSettings value) throws java.io.IOException {
    value.validate();
    PowerConfig.save("maps", value);
    preview(value);
  }

  public static void queued(MapSettings value) {
    value.validate();
    PowerConfig.saveDeferred("maps", value);
    preview(value);
  }
}
