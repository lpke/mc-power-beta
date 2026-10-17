package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import org.junit.jupiter.api.Test;

class TitleTilesTest {
  private JsonArray lines(String... rows) { JsonArray a = new JsonArray(); for (String row : rows) a.add(row); return a; }
  @Test void paintingPreservesOtherCellsAndBlockAssignmentsAcrossSerialization() {
    var lines = lines("*  *", " *"); var palette = new JsonObject(); palette.addProperty("*", 1);
    TitleTiles.paint(lines, palette, 2, 1, 45);
    assertEquals(45, TitleTiles.block(lines, palette, 2, 1));
    assertEquals(1, TitleTiles.block(lines, palette, 1, 1));
    TitleTiles.paint(lines, palette, 0, 0, 0);
    assertEquals(0, TitleTiles.block(lines, palette, 0, 0));
    var readLines = JsonParser.parseString(new Gson().toJson(lines)).getAsJsonArray();
    var readPalette = JsonParser.parseString(new Gson().toJson(palette)).getAsJsonObject();
    assertEquals(45, TitleTiles.block(readLines, readPalette, 2, 1));
    TitleTiles.validateLines(readLines); TitleTiles.validatePalette(readPalette, id -> id == 1 || id == 45);
  }
  @Test void shrinkingCannotEraseTilesAndFailedResizeLeavesTheOriginalIntact() {
    var lines = lines("  * ", " *  "); var original = lines.deepCopy();
    assertThrows(IllegalArgumentException.class, () -> TitleTiles.resize(lines, 2, 2));
    assertThrows(IllegalArgumentException.class, () -> TitleTiles.resize(lines, 4, 1));
    assertEquals(original, lines);
    assertEquals(lines("  *", " * "), TitleTiles.resize(lines, 3, 2));
    assertEquals(12, TitleTiles.resize(lines, 80, 12).size());
  }
  @Test void paletteReusesBlocksAndReclaimsOnlyUnusedCharacters() {
    var grid = lines(" *"); var palette = new JsonObject(); palette.addProperty("*", 1);
    for (char c = '!'; palette.size() < 64; c++) if (c != '*') palette.addProperty(String.valueOf(c), (int)c + 100);
    TitleTiles.paint(grid, palette, 0, 0, 45);
    assertEquals(1, TitleTiles.block(grid, palette, 1, 0));
    assertEquals(45, TitleTiles.block(grid, palette, 0, 0));
    int count = palette.size(); TitleTiles.paint(grid, palette, 1, 0, 45); assertEquals(count, palette.size());
  }
  @Test void malformedAndUnavailableDataAreRejected() {
    assertThrows(IllegalArgumentException.class, () -> TitleTiles.validateLines(lines("x\ny")));
    assertThrows(IllegalArgumentException.class, () -> TitleTiles.validateLines(lines("*".repeat(81))));
    var map = new JsonObject(); map.addProperty("*", 9999);
    assertThrows(IllegalArgumentException.class, () -> TitleTiles.validatePalette(map, id -> id == 1));
    assertThrows(IllegalArgumentException.class, () -> TitleTiles.validatePresets(JsonParser.parseString("[{}]"), id -> true));
  }
}
