package local.luke.power.creative;

import com.google.gson.*;
import java.util.*;

/** Settings-only catalog layouts. Empty strings are intentional empty cells. */
public final class CreativeTabsData {
  public static final int FORMAT = 1, COLUMNS = 8, DEFAULT_ROWS = 8, MAX_TABS = 128, MAX_ROWS = 256;
  private final JsonObject document;
  private CreativeTabsData(JsonObject document) { this.document = document; }
  public static JsonObject defaults() {
    JsonObject value = new JsonObject(); value.addProperty("format", FORMAT);
    value.addProperty("customized", false); value.add("tabs", new JsonArray()); return value;
  }
  public static CreativeTabsData read(JsonObject source) {
    if (source.size() == 0) source = defaults();
    if (source.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 1048576)
      throw new IllegalArgumentException("Creative tabs exceed 1 MiB. Remove tabs or rows before saving.");
    if (!source.has("format") || !source.get("format").isJsonPrimitive()
        || !source.getAsJsonPrimitive("format").isNumber() || source.get("format").getAsDouble() != FORMAT)
      throw new IllegalArgumentException("Unsupported Creative tabs format. The saved layout has been preserved.");
    if (!source.has("customized") || !source.get("customized").isJsonPrimitive()
        || !source.getAsJsonPrimitive("customized").isBoolean() || !source.has("tabs") || !source.get("tabs").isJsonArray())
      throw new IllegalArgumentException("Invalid Creative tabs layout");
    JsonArray tabs = source.getAsJsonArray("tabs");
    if (tabs.size() > MAX_TABS) throw new IllegalArgumentException("Too many Creative tabs");
    Set<String> ids = new HashSet<>();
    for (JsonElement value : tabs) {
      if (!value.isJsonObject()) throw new IllegalArgumentException("Invalid Creative tab");
      JsonObject tab = value.getAsJsonObject();
      String id = string(tab, "id"), title = string(tab, "title"), icon = string(tab, "icon");
      if (id.isBlank() || id.length() > 128 || !ids.add(id)) throw new IllegalArgumentException("Invalid or duplicate Creative tab ID");
      if (title.isBlank() || title.length() > 64) throw new IllegalArgumentException("Tab names need 1 to 64 characters");
      validateItem(icon);
      if (!tab.has("slots") || !tab.get("slots").isJsonArray()) throw new IllegalArgumentException("Invalid Creative tab slots");
      JsonArray slots = tab.getAsJsonArray("slots");
      if (slots.size() > COLUMNS * MAX_ROWS) throw new IllegalArgumentException("Too many Creative tab rows");
      for (JsonElement cell : slots) {
        if (!cell.isJsonPrimitive() || !cell.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("Invalid Creative tab item");
        if (!cell.getAsString().isEmpty()) validateItem(cell.getAsString());
      }
    }
    return new CreativeTabsData(source.deepCopy());
  }
  private static String string(JsonObject object, String key) {
    JsonElement value = object.get(key);
    if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
      throw new IllegalArgumentException("Invalid Creative tab " + key);
    return value.getAsString();
  }
  public static void validateItem(String value) {
    if (value.length() > 256 || !(value.matches("[1-9][0-9]*(?::[0-9]+)?")
        || value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))) throw new IllegalArgumentException("Invalid item ID: " + value);
    if (value.matches("[0-9]+(?::[0-9]+)?")) try {
      String[] pieces = value.split(":"); Integer.parseInt(pieces[0]);
      if (pieces.length == 2 && Integer.parseInt(pieces[1]) > 32767) throw new NumberFormatException();
    } catch (NumberFormatException invalid) { throw new IllegalArgumentException("Item ID or metadata is outside its supported range"); }
  }
  public JsonObject json() { return document.deepCopy(); }
  public boolean customized() { return document.get("customized").getAsBoolean(); }
  public void customized(boolean customized) { document.addProperty("customized", customized); }
  public int size() { return document.getAsJsonArray("tabs").size(); }
  public JsonObject tab(int index) { return document.getAsJsonArray("tabs").get(index).getAsJsonObject(); }
  public void append(JsonObject tab) {
    if (size() >= MAX_TABS) throw new IllegalArgumentException("At most " + MAX_TABS + " tabs are allowed");
    document.getAsJsonArray("tabs").add(tab.deepCopy());
  }
  public void remove(int index) { document.getAsJsonArray("tabs").remove(index); }
  public void move(int from, int to) {
    if (from == to) return;
    JsonArray old = document.getAsJsonArray("tabs"), next = new JsonArray();
    JsonElement selected = old.get(from);
    List<JsonElement> values = new ArrayList<>(); old.forEach(values::add);
    values.remove(from); values.add(to, selected); values.forEach(next::add); document.add("tabs", next);
  }
  public static JsonObject tab(String id, String title, String icon, Collection<String> values) {
    JsonObject tab = new JsonObject(); tab.addProperty("id", id); tab.addProperty("title", title); tab.addProperty("icon", icon);
    JsonArray slots = new JsonArray(); values.forEach(slots::add); tab.add("slots", slots); return tab;
  }
  public static List<String> slots(JsonObject tab) {
    List<String> result = new ArrayList<>(); tab.getAsJsonArray("slots").forEach(value -> result.add(value.getAsString())); return result;
  }
  public static void slots(JsonObject tab, List<String> slots) {
    JsonArray values = new JsonArray(); slots.forEach(values::add); tab.add("slots", values);
  }
  /** Moving a selection preserves its spacing and swaps displaced cells into vacated positions. */
  public static List<String> moveCells(List<String> source, Collection<Integer> selection, int delta) {
    List<String> result = new ArrayList<>(source);
    SortedSet<Integer> selected = new TreeSet<>(selection);
    if (selected.isEmpty() || delta == 0) return result;
    if (selected.first() < 0 || selected.last() >= source.size() || selected.first() + delta < 0
        || selected.last() + delta >= source.size()) throw new IllegalArgumentException("Selection is outside the grid");
    Set<Integer> destinations = new HashSet<>(); selected.forEach(i -> destinations.add(i + delta));
    // Displaced cells swap into the vacated selected positions, preserving every item.
    List<Integer> vacated = selected.stream().filter(i -> !destinations.contains(i)).toList();
    List<Integer> displaced = destinations.stream().filter(i -> !selected.contains(i)).sorted().toList();
    for (int i : selected) result.set(i + delta, source.get(i));
    for (int i = 0; i < vacated.size(); i++) result.set(vacated.get(i), source.get(displaced.get(i)));
    return result;
  }
}
