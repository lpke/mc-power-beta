package local.luke.power.hud;

import local.luke.power.PowerBeta;
import local.luke.power.config.Catalog;
import local.luke.power.storage.PowerConfig;

public final class InfoHudConfig {
  private static InfoHudSettings current = load();
  private InfoHudConfig() {}
  private static InfoHudSettings load() {
    try {
      var value = PowerConfig.read("infoHud", InfoHudSettings.class);
      value.validate();
      return value;
    } catch (Exception e) {
      PowerBeta.LOG.error("Could not read custom HUD settings; preserving the file", e);
      return new InfoHudSettings();
    }
  }
  public static InfoHudSettings current() { return current; }
  public static InfoHudSettings copy() { return copy(current); }
  private static InfoHudSettings copy(InfoHudSettings value) {
    return Catalog.JSON.fromJson(Catalog.JSON.toJson(value), InfoHudSettings.class);
  }
  public static void preview(InfoHudSettings value) { value.validate(); current = copy(value); }
  public static void save(InfoHudSettings value) throws java.io.IOException {
    value.validate(); PowerConfig.save("infoHud", value); preview(value);
  }
  public static void toggle() {
    var next = copy(); next.enabled = !next.enabled;
    next.validate();
    PowerConfig.saveDeferred("infoHud", next);
    preview(next);
  }
}
