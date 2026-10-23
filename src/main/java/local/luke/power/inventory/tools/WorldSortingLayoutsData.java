package local.luke.power.inventory.tools;

import com.google.gson.*;
import java.util.*;

/** Stable world IDs own layouts. The retained seed is never changed by a world's edits. */
public final class WorldSortingLayoutsData {
  public static final String SECTION = "worldSortingLayouts";
  private final JsonObject root;
  private WorldSortingLayoutsData(JsonObject root) { this.root = root.deepCopy(); }
  public static WorldSortingLayoutsData read(JsonObject source) {
    if (!source.has("format") || !source.get("format").isJsonPrimitive()
        || !source.getAsJsonPrimitive("format").isNumber() || source.get("format").getAsBigDecimal().intValueExact() != 1)
      throw new IllegalArgumentException("Unsupported world sorting layouts. Retain the original settings file.");
    if (!source.has("legacy") || !source.get("legacy").isJsonObject()
        || !source.has("worlds") || !source.get("worlds").isJsonObject())
      throw new IllegalArgumentException("Invalid world sorting layouts. Retain the original settings file.");
    return new WorldSortingLayoutsData(source);
  }
  public static WorldSortingLayoutsData migrate(JsonObject settings) {
    if (settings.has(SECTION)) return read(settings.getAsJsonObject(SECTION));
    JsonObject legacy = new JsonObject();
    JsonObject tools = settings.has("inventoryTools") ? settings.getAsJsonObject("inventoryTools") : new JsonObject();
    legacy.add("protectedSlots", tools.has("protectedSlots") ? tools.get("protectedSlots").deepCopy() : new JsonArray());
    legacy.add("preferredItems", tools.has("preferredItems") ? tools.get("preferredItems").deepCopy() : new JsonObject());
    if (settings.has("sortingLayouts")) legacy.add("presets", settings.get("sortingLayouts").deepCopy());
    JsonObject root = new JsonObject(); root.addProperty("format", 1); root.add("legacy", legacy); root.add("worlds", new JsonObject());
    return read(root);
  }
  public JsonObject json() { return root.deepCopy(); }
  public boolean contains(String world) { identity(world); return root.getAsJsonObject("worlds").has(world); }
  public JsonObject layout(String world) {
    identity(world);
    JsonElement stored = root.getAsJsonObject("worlds").get(world);
    JsonObject value = (stored == null ? root.getAsJsonObject("legacy") : stored.getAsJsonObject()).deepCopy();
    SortingLayoutsData.layout(value.get("protectedSlots"), value.get("preferredItems"));
    return value;
  }
  public WorldSortingLayoutsData seed(Collection<String> worlds) {
    JsonObject next = json();
    for (String world : worlds) {
      identity(world);
      if (!next.getAsJsonObject("worlds").has(world)) next.getAsJsonObject("worlds").add(world, root.getAsJsonObject("legacy").deepCopy());
    }
    return read(next);
  }
  public WorldSortingLayoutsData replace(String world, JsonObject value) {
    identity(world); SortingLayoutsData.layout(value.get("protectedSlots"), value.get("preferredItems"));
    JsonObject next = json(); next.getAsJsonObject("worlds").add(world, value.deepCopy()); return read(next);
  }
  private static void identity(String world) {
    if (world == null || !UUID.fromString(world).toString().equals(world)) throw new IllegalArgumentException("Invalid sorting-layout world identity");
  }
}
