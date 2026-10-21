package local.luke.power.inventory.tools;

import com.google.gson.*;
import java.util.*;

/** Bounded, versioned user presets independent of defaults and live inventory. */
public final class SortingLayoutsData {
  public static final int FORMAT = 1, LIMIT = 32;
  private final JsonObject root;
  private SortingLayoutsData(JsonObject root) { this.root = root.deepCopy(); }
  public static SortingLayoutsData read(JsonObject source) {
    JsonObject root = source.deepCopy();
    if (root.size() == 0) { root.addProperty("format", FORMAT); root.add("presets", new JsonArray()); }
    if (!root.has("format") || !number(root.get("format")) || root.get("format").getAsBigDecimal().intValueExact() != FORMAT)
      throw new IllegalArgumentException("Unsupported sorting preset format. Original presets retained.");
    if (!root.has("presets") || !root.get("presets").isJsonArray() || root.getAsJsonArray("presets").size() > LIMIT || root.toString().length() > 262144)
      throw new IllegalArgumentException("Invalid sorting presets. Original presets retained.");
    Set<String> ids = new HashSet<>();
    for (JsonElement element : root.getAsJsonArray("presets")) {
      if (!element.isJsonObject()) throw new IllegalArgumentException("Invalid sorting preset");
      JsonObject p = element.getAsJsonObject(); String id = text(p, "id"), name = text(p, "name");
      if (!id.matches("[A-Za-z0-9_-]{1,64}") || !ids.add(id) || name.isBlank() || name.length() > 64 || name.chars().anyMatch(Character::isISOControl))
        throw new IllegalArgumentException("Invalid sorting preset name or ID");
      layout(p.get("protectedSlots"), p.get("preferredItems"));
    }
    return new SortingLayoutsData(root);
  }
  public static void layout(JsonElement protectedSlots, JsonElement preferredItems) {
    if (protectedSlots == null || !protectedSlots.isJsonArray() || protectedSlots.getAsJsonArray().size() > 27
        || preferredItems == null || !preferredItems.isJsonObject() || preferredItems.getAsJsonObject().size() > 27)
      throw new IllegalArgumentException("Invalid sorting layout");
    Set<Integer> seen = new HashSet<>();
    for (JsonElement slot : protectedSlots.getAsJsonArray()) {
      if (!number(slot)) throw new IllegalArgumentException("Invalid protected slot");
      int n = slot.getAsBigDecimal().intValueExact();
      if (n < 9 || n > 35 || !seen.add(n)) throw new IllegalArgumentException("Invalid protected slot");
    }
    for (var entry : preferredItems.getAsJsonObject().entrySet()) {
      if (!entry.getKey().matches("[1-3][0-9]|9")) throw new IllegalArgumentException("Invalid preferred slot");
      int slot = Integer.parseInt(entry.getKey());
      if (slot > 35 || !entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString()
          || !entry.getValue().getAsString().matches("[1-9][0-9]{0,8}(:[0-9]{1,5})?")) throw new IllegalArgumentException("Invalid preferred item");
    }
  }
  private static boolean number(JsonElement e) { return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber(); }
  private static String text(JsonObject o, String key) {
    JsonElement e = o.get(key);
    if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("Invalid sorting preset " + key);
    return e.getAsString();
  }
  public JsonObject json() { return root.deepCopy(); }
  public int size() { return root.getAsJsonArray("presets").size(); }
  public JsonObject preset(int index) { return root.getAsJsonArray("presets").get(index).getAsJsonObject().deepCopy(); }
  public SortingLayoutsData save(String name, JsonElement protectedSlots, JsonElement preferredItems) {
    JsonObject next = json(), preset = new JsonObject();
    preset.addProperty("id", UUID.randomUUID().toString()); preset.addProperty("name", name.strip());
    preset.add("protectedSlots", protectedSlots.deepCopy()); preset.add("preferredItems", preferredItems.deepCopy());
    next.getAsJsonArray("presets").add(preset); return read(next);
  }
  public SortingLayoutsData delete(int index) { JsonObject next = json(); next.getAsJsonArray("presets").remove(index); return read(next); }
}
