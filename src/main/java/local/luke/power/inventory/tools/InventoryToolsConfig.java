package local.luke.power.inventory.tools;

import local.luke.power.config.Catalog;
import local.luke.power.storage.PowerConfig;

public final class InventoryToolsConfig {
  private static InventoryToolsSettings current = PowerConfig.read("inventoryTools", InventoryToolsSettings.class);
  private static final java.util.List<Integer> legacyProtected = new java.util.ArrayList<>(current.protectedSlots);
  private static final java.util.Map<Integer, String> legacyPreferred = new java.util.LinkedHashMap<>(current.preferredItems);
  private InventoryToolsConfig() {}
  static void worldChanged() { WorldSortingLayouts.applyTo(current); }
  private static InventoryToolsSettings global(InventoryToolsSettings value) {
    var saved = Catalog.JSON.fromJson(Catalog.JSON.toJson(value), InventoryToolsSettings.class);
    saved.protectedSlots = new java.util.ArrayList<>(legacyProtected);
    saved.preferredItems = new java.util.LinkedHashMap<>(legacyPreferred); return saved;
  }
  public static InventoryToolsSettings current() { return current; }
  public static InventoryToolsSettings copy() { return copy(current); }
  private static InventoryToolsSettings copy(InventoryToolsSettings value) {
    var next = Catalog.JSON.fromJson(Catalog.JSON.toJson(value), InventoryToolsSettings.class);
    next.restock = value.restock; next.toolSwitch = value.toolSwitch;
    return next;
  }
  public static void preview(InventoryToolsSettings value) { value.validate(); current = copy(value); }
  public static void save(InventoryToolsSettings value) throws java.io.IOException { value.validate(); WorldSortingLayouts.saveLayout(value, false); PowerConfig.save("inventoryTools", global(value)); preview(value); }
  public static void queuedPreferences(InventoryToolsSettings value) { value.validate(); PowerConfig.saveDeferred("inventoryTools", global(value)); preview(value); }
  public static void queuedLayout(java.util.Set<Integer> protectedSlots, java.util.Map<Integer, String> preferredItems) {
    var next = copy(); next.protectedSlots = new java.util.ArrayList<>(protectedSlots);
    next.preferredItems = new java.util.LinkedHashMap<>(preferredItems); next.validate();
    try { WorldSortingLayouts.saveLayout(next, true); }
    catch (java.io.IOException failure) { throw new IllegalStateException("Sorting layout could not be queued", failure); }
    preview(next);
  }
}
