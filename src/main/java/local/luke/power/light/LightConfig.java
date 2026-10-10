package local.luke.power.light;

import local.luke.power.PowerBeta;
import local.luke.power.config.Catalog;
import local.luke.power.storage.PowerConfig;

public final class LightConfig {
  private static LightSettings current = load();

  private static LightSettings load() {
    try {
      LightSettings settings = PowerConfig.read("lightOverlay", LightSettings.class);
      settings.validate();
      return settings;
    } catch (Exception e) {
      PowerBeta.LOG.error("Could not load light overlay settings", e);
      return new LightSettings();
    }
  }

  public static LightSettings current() { return current; }
  public static LightSettings copy() { return copy(current); }
  private static LightSettings copy(LightSettings settings) {
    return Catalog.JSON.fromJson(Catalog.JSON.toJson(settings), LightSettings.class);
  }
  public static void preview(LightSettings settings) {
    settings.validate();
    current = copy(settings);
  }
  public static void save(LightSettings settings) throws Exception {
    settings.validate();
    PowerConfig.save("lightOverlay", settings);
    preview(settings);
  }
  public static void toggle() {
    LightSettings settings = copy(); settings.enabled = !settings.enabled;
    settings.validate();
    PowerConfig.saveDeferred("lightOverlay", settings);
    preview(settings);
  }
}
