package local.luke.power.inventory.tools;

import com.google.gson.*;
import java.util.*;

/** Versioned user data. Missing registry items remain encoded until an explicit row overwrite. */
public final class SavedCreativeHotbarsData {
  public static final int FORMAT = 1, ROWS = 9, SLOTS = 9, MAX_TOTAL = 512 * 1024, MAX_ITEM = 128 * 1024;
  private final String[][] bars;
  private final JsonObject retained;
  private SavedCreativeHotbarsData(String[][] bars, JsonObject retained) { this.bars = copy(bars); this.retained = retained.deepCopy(); validate(); }
  public static SavedCreativeHotbarsData empty() { return new SavedCreativeHotbarsData(new String[ROWS][SLOTS], new JsonObject()); }
  public static SavedCreativeHotbarsData read(JsonObject root) {
    if (root == null) throw new IllegalArgumentException("Saved hotbar data is missing");
    if (root.size() == 0) return empty();
    if (!root.has("format") || !root.get("format").isJsonPrimitive() || !root.getAsJsonPrimitive("format").isNumber()
        || root.get("format").getAsBigDecimal().intValueExact() != FORMAT)
      throw new IllegalArgumentException("Unsupported saved hotbar format. Use a compatible Power Beta release; the original presets are retained.");
    if (!root.has("bars") || !root.get("bars").isJsonArray() || root.getAsJsonArray("bars").size() != ROWS)
      throw new IllegalArgumentException("Saved hotbars must contain exactly nine rows");
    String[][] bars = new String[ROWS][SLOTS];
    for (int row = 0; row < ROWS; row++) {
      JsonElement value = root.getAsJsonArray("bars").get(row);
      if (!value.isJsonArray() || value.getAsJsonArray().size() != SLOTS) throw new IllegalArgumentException("Saved hotbars must contain nine slots per row");
      for (int slot = 0; slot < SLOTS; slot++) {
        JsonElement item = value.getAsJsonArray().get(slot);
        if (item.isJsonNull()) continue;
        if (!item.isJsonPrimitive() || !item.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("Invalid saved hotbar item data");
        bars[row][slot] = item.getAsString();
      }
    }
    return new SavedCreativeHotbarsData(bars, root);
  }
  public String[] bar(int row) { row(row); return bars[row].clone(); }
  public SavedCreativeHotbarsData with(int row, String[] items) {
    row(row); if (items == null || items.length != SLOTS) throw new IllegalArgumentException("A saved hotbar has nine slots");
    String[][] next = copy(bars); next[row] = items.clone(); return new SavedCreativeHotbarsData(next, retained);
  }
  public JsonObject json() {
    JsonObject root = retained.deepCopy(); root.addProperty("format", FORMAT); JsonArray rows = new JsonArray();
    for (String[] bar : bars) {
      JsonArray items = new JsonArray(); for (String item : bar) items.add(item == null ? JsonNull.INSTANCE : new JsonPrimitive(item)); rows.add(items);
    }
    root.add("bars", rows); return root;
  }
  private void validate() {
    int total = 0;
    for (String[] row : bars) for (String item : row) if (item != null) {
      if (item.isEmpty() || item.length() > MAX_ITEM || (total += item.length()) > MAX_TOTAL) throw new IllegalArgumentException("Saved hotbars exceed their bounded storage size");
      try { Base64.getDecoder().decode(item); } catch (IllegalArgumentException invalid) { throw new IllegalArgumentException("Saved hotbars contain invalid item data; original presets retained", invalid); }
    }
    if (retained.toString().length() > MAX_TOTAL + 16384) throw new IllegalArgumentException("Saved hotbar metadata exceeds its storage limit");
  }
  private static void row(int row) { if (row < 0 || row >= ROWS) throw new IllegalArgumentException("Choose saved hotbar 1 through 9"); }
  private static String[][] copy(String[][] source) { String[][] result = new String[source.length][]; for (int i = 0; i < source.length; i++) result[i] = source[i].clone(); return result; }
}
