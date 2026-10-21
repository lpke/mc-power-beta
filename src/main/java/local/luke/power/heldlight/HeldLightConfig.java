package local.luke.power.heldlight;

import local.luke.power.config.Catalog;
import local.luke.power.storage.PowerConfig;

public final class HeldLightConfig {
  private static HeldLightSettings current = PowerConfig.read("heldLight", HeldLightSettings.class);
  private HeldLightConfig() {}
  public static HeldLightSettings current() { return current; }
  public static HeldLightSettings copy() { return Catalog.JSON.fromJson(Catalog.JSON.toJson(current), HeldLightSettings.class); }
  public static void preview(HeldLightSettings value) { current = Catalog.JSON.fromJson(Catalog.JSON.toJson(value), HeldLightSettings.class); }
  public static void save(HeldLightSettings value) throws Exception { PowerConfig.save("heldLight", value); preview(value); }
}
