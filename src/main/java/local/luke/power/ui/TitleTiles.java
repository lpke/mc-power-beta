package local.luke.power.ui;

import com.google.gson.*;
import java.util.*;
import java.util.function.IntPredicate;

/** Bounded tile edits retain the legacy character grid and palette on disk. */
public final class TitleTiles {
  private TitleTiles() {}
  public static int width(JsonArray lines) {
    int width = 1;
    for (JsonElement line : lines) width = Math.max(width, line.getAsString().length());
    return width;
  }
  public static int block(JsonArray lines, JsonObject palette, int x, int y) {
    String row = lines.get(y).getAsString();
    if (x >= row.length()) return 0;
    JsonElement value = palette.get(String.valueOf(row.charAt(x)));
    return value == null ? 0 : value.getAsInt();
  }
  public static void paint(JsonArray lines, JsonObject palette, int x, int y, int block) {
    if (x < 0 || x >= width(lines) || y < 0 || y >= lines.size() || block < 0)
      throw new IllegalArgumentException("Invalid title tile");
    char symbol = ' ';
    if (block != 0) {
      for (var entry : palette.entrySet()) if (!entry.getKey().equals(" ") && entry.getValue().getAsInt() == block) {
        symbol = entry.getKey().charAt(0); break;
      }
      if (symbol == ' ') {
        if (palette.size() >= 64) {
          Set<String> used = new HashSet<>();
          lines.forEach(line -> line.getAsString().chars().forEach(c -> used.add(String.valueOf((char)c))));
          List<String> unused = palette.keySet().stream().filter(c -> !used.contains(c)).toList();
          unused.forEach(palette::remove);
        }
        if (palette.size() >= 64) throw new IllegalArgumentException("A title can use up to 64 different blocks");
        for (char c = '!'; c < 127; c++) if (!palette.has(String.valueOf(c))) { symbol = c; break; }
        palette.addProperty(String.valueOf(symbol), block);
      }
    }
    int w = width(lines);
    String row = lines.get(y).getAsString();
    char[] cells = (row + " ".repeat(w - row.length())).toCharArray();
    cells[x] = symbol;
    lines.set(y, new JsonPrimitive(new String(cells)));
  }
  public static JsonArray resize(JsonArray lines, int width, int height) {
    if (width < 1 || width > 80 || height < 1 || height > 12)
      throw new IllegalArgumentException("Use 1–80 columns and 1–12 rows");
    JsonArray next = new JsonArray();
    for (int y = 0; y < Math.max(height, lines.size()); y++) {
      String row = y < lines.size() ? lines.get(y).getAsString() : "";
      if (y >= height && !row.isBlank() || row.length() > width && !row.substring(width).isBlank())
        throw new IllegalArgumentException("Erase occupied tiles before shrinking the grid");
      if (y < height) next.add(row.substring(0, Math.min(width, row.length())) + " ".repeat(Math.max(0, width - row.length())));
    }
    return next;
  }
  public static void validateLines(JsonElement value) {
    if (!value.isJsonArray() || value.getAsJsonArray().size() < 1 || value.getAsJsonArray().size() > 12)
      throw new IllegalArgumentException("Use 1 to 12 title rows");
    for (JsonElement row : value.getAsJsonArray())
      if (!row.isJsonPrimitive() || !row.getAsJsonPrimitive().isString() || row.getAsString().length() > 80
          || row.getAsString().chars().anyMatch(c -> Character.isISOControl(c)))
        throw new IllegalArgumentException("Title rows must be text up to 80 characters");
  }
  public static void validatePalette(JsonElement value, IntPredicate validBlock) {
    if (!value.isJsonObject() || value.getAsJsonObject().size() > 64)
      throw new IllegalArgumentException("Use up to 64 title blocks");
    for (var entry : value.getAsJsonObject().entrySet()) {
      JsonElement id = entry.getValue();
      if (entry.getKey().length() != 1 || !id.isJsonPrimitive() || !id.getAsJsonPrimitive().isNumber()
          || !validBlock.test(id.getAsBigDecimal().intValueExact()))
        throw new IllegalArgumentException("Use single letters and existing block IDs");
    }
  }
  public static void validateSwatches(JsonElement value, IntPredicate validBlock) {
    if (!value.isJsonArray() || value.getAsJsonArray().size() > 16) throw new IllegalArgumentException("Save up to 16 brush swatches");
    Set<Integer> ids = new HashSet<>();
    for (JsonElement entry : value.getAsJsonArray()) {
      if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Use block IDs for swatches");
      int id = entry.getAsBigDecimal().intValueExact();
      if (!validBlock.test(id) || !ids.add(id)) throw new IllegalArgumentException("Use different valid blocks for swatches");
    }
  }
  public static final Set<String> DESIGN_KEYS = Set.of("logo.logo.lines", "logo.blocks.map", "logo.logo.rotation",
      "logo.blocks.inverted", "logo.logo.shadow", "logo.logo.animation.enabled", "logo.logo.animation.speed");
  public static void validatePresets(JsonElement value, IntPredicate validBlock) {
    if (!value.isJsonArray() || value.getAsJsonArray().size() > 24) throw new IllegalArgumentException("Save up to 24 title presets");
    Set<String> ids = new HashSet<>();
    for (JsonElement entry : value.getAsJsonArray()) {
      if (!entry.isJsonObject() || !entry.getAsJsonObject().has("id") || !entry.getAsJsonObject().has("name")
          || !entry.getAsJsonObject().has("values")) throw new IllegalArgumentException("Invalid title preset");
      JsonObject preset = entry.getAsJsonObject();
      String id = preset.get("id").getAsString(), name = preset.get("name").getAsString();
      if (!id.equals(UUID.fromString(id).toString()) || !ids.add(id) || name.isBlank() || name.length() > 48
          || name.chars().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("Invalid title preset name or ID");
      validateDesign(preset.getAsJsonObject("values"), validBlock);
    }
  }
  public static void validateDesign(JsonObject design, IntPredicate validBlock) {
      if (!design.keySet().equals(DESIGN_KEYS)) throw new IllegalArgumentException("Incomplete title preset");
      validateLines(design.get("logo.logo.lines")); validatePalette(design.get("logo.blocks.map"), validBlock);
      JsonArray rotation = design.getAsJsonArray("logo.logo.rotation");
      if (rotation.size() != 3) throw new IllegalArgumentException("Use three rotation angles");
      for (JsonElement angle : rotation) if (!Double.isFinite(angle.getAsDouble()) || Math.abs(angle.getAsDouble()) > 360)
        throw new IllegalArgumentException("Angles must be -360 to 360");
      for (String key : List.of("logo.blocks.inverted", "logo.logo.shadow", "logo.logo.animation.enabled"))
        if (!design.get(key).isJsonPrimitive() || !design.getAsJsonPrimitive(key).isBoolean()) throw new IllegalArgumentException("Invalid title toggle");
      double speed = design.get("logo.logo.animation.speed").getAsDouble();
      if (!Double.isFinite(speed) || speed < 0 || speed > 10) throw new IllegalArgumentException("Invalid animation speed");
  }

}
