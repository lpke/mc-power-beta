package local.luke.power.status;

import local.luke.power.PowerBeta;
import local.luke.power.config.Catalog;
import local.luke.power.storage.PowerConfig;

public final class StatusConfig {
  private static StatusSettings current = load();
  private StatusConfig() {}
  private static StatusSettings load() {
    try {
      StatusSettings value = PowerConfig.read("activeTweaks", StatusSettings.class);
      value.validate();
      return value;
    } catch (Exception e) {
      PowerBeta.LOG.error("Could not load active-tweaks settings; preserving file", e);
      return new StatusSettings();
    }
  }
  public static StatusSettings current() { return current; }
  public static StatusSettings copy() { return copy(current); }
  private static StatusSettings copy(StatusSettings value) {
    return Catalog.JSON.fromJson(Catalog.JSON.toJson(value), StatusSettings.class);
  }
  public static void preview(StatusSettings value) { value.validate(); current = copy(value); }
  public static void save(StatusSettings value) throws Exception {
    value.validate(); PowerConfig.save("activeTweaks", value); preview(value);
  }
}
