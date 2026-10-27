package local.luke.power.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import java.util.List;
import local.luke.power.config.Setting;

/** Dimension controls share validation while failed resizes retain the painted draft. */
final class TitleGridDimensions {
  final Setting columns = dimension("columns", "Grid columns", 38, 80);
  final Setting rows = dimension("rows", "Grid rows", 5, 12);
  private final Setting lines;

  TitleGridDimensions(Setting lines) {
    this.lines = lines;
    columns.validator(value -> TitleTiles.resize(grid(), value.getAsBigDecimal().intValueExact(), grid().size()));
    rows.validator(value -> TitleTiles.resize(grid(), TitleTiles.width(grid()), value.getAsBigDecimal().intValueExact()));
    sync();
  }

  private static Setting dimension(String id, String label, int value, int maximum) {
    return new Setting("titleEditor." + id, "", "", "", label, "", Setting.Kind.INTEGER,
        new JsonPrimitive(value), new JsonPrimitive(value), 1, maximum, 1, List.of(), false);
  }

  private JsonArray grid() { return lines.value.getAsJsonArray(); }
  boolean owns(Setting setting) { return setting == columns || setting == rows; }

  void sync() {
    columns.value = new JsonPrimitive(TitleTiles.width(grid()));
    rows.value = new JsonPrimitive(grid().size());
  }

  String apply(Setting changed) {
    try {
      // Slider and reset edits assign directly, unlike exact entry's Setting.parse.
      changed.validate(changed.value);
      lines.value = TitleTiles.resize(grid(), columns.value.getAsInt(), rows.value.getAsInt());
      return "";
    } catch (IllegalArgumentException | ArithmeticException e) {
      return e.getMessage();
    } finally {
      sync();
    }
  }
}
