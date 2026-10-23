package local.luke.power.inventory.tools;

import com.google.gson.*;
import java.io.IOException;
import java.util.*;
import local.luke.power.storage.PowerConfig;

/** Only load boundaries read storage. Gameplay editors queue owned immutable settings snapshots. */
public final class WorldSortingLayouts {
  private static WorldSortingLayoutsData data;
  private static String world;
  private static String problem = "";
  private WorldSortingLayouts() {}
  public static void initialize() {
    if (data != null || !problem.isEmpty()) return;
    try {
      JsonObject settings = PowerConfig.document().getAsJsonObject("settings");
      data = WorldSortingLayoutsData.migrate(settings);
      if (!settings.has(WorldSortingLayoutsData.SECTION)) PowerConfig.replaceSection(WorldSortingLayoutsData.SECTION, null, data.json());
    } catch (IOException | RuntimeException failure) { problem = "World sorting layouts are unavailable: " + failure.getMessage(); }
  }
  public static void activate(String identity, java.nio.file.Path directory) throws IOException {
    initialize(); world = null;
    if (data == null) return;
    try {
      var next = data;
      if (!next.contains(identity)) {
        JsonObject imported = WorldSortingLayoutCopies.imported(directory, identity);
        next = imported == null ? next.seed(List.of(identity)) : next.replace(identity, imported);
      }
      next.layout(identity);
      if (!next.json().equals(data.json())) PowerConfig.replaceSection(WorldSortingLayoutsData.SECTION, data.json(), next.json());
      data = next; world = identity;
    } catch (IOException | RuntimeException unsupported) { problem = "World sorting layouts are unavailable: " + unsupported.getMessage(); }
    InventoryToolsConfig.worldChanged();
  }
  public static void clear() { world = null; InventoryToolsConfig.worldChanged(); }
  public static boolean available() { return data != null && world != null; }
  public static String problem() { return problem; }
  public static String world() { return world; }
  public static void requireWorld(String expected) {
    if (expected == null || !Objects.equals(world, expected) || !available())
      throw new IllegalStateException("The world changed. Reopen the sorting editor before saving.");
  }
  static JsonObject layout() {
    if (!available()) throw new IllegalStateException(problem.isEmpty() ? "Open a singleplayer world to edit its sorting layout" : problem);
    return data.layout(world);
  }
  static void applyTo(InventoryToolsSettings settings) {
    settings.protectedSlots = new ArrayList<>(); settings.preferredItems = new LinkedHashMap<>();
    if (!available()) return;
    JsonObject layout = layout();
    for (var value : layout.getAsJsonArray("protectedSlots")) settings.protectedSlots.add(value.getAsInt());
    for (var value : layout.getAsJsonObject("preferredItems").entrySet()) settings.preferredItems.put(Integer.parseInt(value.getKey()), value.getValue().getAsString());
  }
  static void saveLayout(InventoryToolsSettings settings, boolean queued) throws IOException {
    if (!available()) return;
    JsonObject next = layout();
    next.add("protectedSlots", local.luke.power.config.Catalog.JSON.toJsonTree(settings.protectedSlots));
    next.add("preferredItems", local.luke.power.config.Catalog.JSON.toJsonTree(settings.preferredItems));
    replace(next, queued);
  }
  static void replace(JsonObject layout, boolean queued) throws IOException {
    var next = data.replace(world, layout);
    if (next.json().equals(data.json())) return;
    if (queued) PowerConfig.saveDeferred(WorldSortingLayoutsData.SECTION, next.json());
    else PowerConfig.replaceSection(WorldSortingLayoutsData.SECTION, data.json(), next.json());
    data = next;
  }
}
