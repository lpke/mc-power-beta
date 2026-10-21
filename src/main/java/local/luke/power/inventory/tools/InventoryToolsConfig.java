package local.luke.power.inventory.tools;

import local.luke.power.config.Catalog;
import local.luke.power.storage.PowerConfig;

public final class InventoryToolsConfig {
  private static InventoryToolsSettings current = PowerConfig.read("inventoryTools", InventoryToolsSettings.class);
  private InventoryToolsConfig() {}
  public static InventoryToolsSettings current() { return current; }
  public static InventoryToolsSettings copy() { return Catalog.JSON.fromJson(Catalog.JSON.toJson(current), InventoryToolsSettings.class); }
  public static void preview(InventoryToolsSettings value) { value.validate(); current = Catalog.JSON.fromJson(Catalog.JSON.toJson(value), InventoryToolsSettings.class); }
  public static void save(InventoryToolsSettings value) throws java.io.IOException { value.validate(); PowerConfig.save("inventoryTools", value); preview(value); }
}
