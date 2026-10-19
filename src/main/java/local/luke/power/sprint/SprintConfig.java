package local.luke.power.sprint;

import local.luke.power.config.Catalog;
import local.luke.power.storage.PowerConfig;

public final class SprintConfig {
  private static SprintSettings current = load();

  private SprintConfig() {}

  private static SprintSettings load() {
    try {
      var s = PowerConfig.read("sprint", SprintSettings.class);
      s.validate();
      return s;
    } catch (Exception e) {
      local.luke.power.PowerBeta.LOG.error("Could not load sprint preferences", e);
      return new SprintSettings();
    }
  }

  public static SprintSettings current() {
    return current;
  }

  public static SprintSettings copy() {
    return Catalog.JSON.fromJson(Catalog.JSON.toJson(current), SprintSettings.class);
  }

  public static void preview(SprintSettings value) {
    value.validate();
    current = Catalog.JSON.fromJson(Catalog.JSON.toJson(value), SprintSettings.class);
  }

  public static void save(SprintSettings value) throws java.io.IOException {
    value.validate();
    PowerConfig.save("sprint", value);
    preview(value);
  }
}
