package local.luke.power.ui;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TitleSelectionTest {
  @Test void cycleIdentifiesSavedPresetsByStableIdAndExactDesignAndRetainsTheBaseSlot() {
    JsonArray presets = new JsonArray(); JsonObject design = new JsonObject(); design.addProperty("logo.logo.shadow", true);
    for (String id : new String[]{"first", "second"}) {
      JsonObject preset = new JsonObject(); preset.addProperty("id", id); preset.add("values", design.deepCopy()); presets.add(preset);
    }
    assertEquals(0, TitleSelection.index(0, "", presets, design));
    assertEquals(1, TitleSelection.index(0, "first", presets, design));
    assertEquals(2, TitleSelection.index(0, "second", presets, design));
    assertEquals(3, TitleSelection.index(1, "second", presets, design));
    assertEquals(6, TitleSelection.index(4, "", presets, design));
    design.addProperty("logo.logo.shadow", false);
    assertEquals(0, TitleSelection.index(0, "first", presets, design));
    assertEquals(0, TitleSelection.index(0, "removed", presets, design));
  }
  @Test void swatchesRejectDuplicatesUnknownBlocksFractionsAndOversizedListsWithRuntimeGson() {
    assertDoesNotThrow(() -> TitleTiles.validateSwatches(JsonParser.parseString("[1,45]"), id -> id == 1 || id == 45));
    for (String json : new String[]{"[1,1]", "[8]", "[1.5]", "[\"1\"]", "{}"})
      assertThrows(RuntimeException.class, () -> TitleTiles.validateSwatches(JsonParser.parseString(json), id -> id == 1));
    JsonArray tooMany = new JsonArray(); for (int i = 1; i < 18; i++) tooMany.add(i);
    assertThrows(IllegalArgumentException.class, () -> TitleTiles.validateSwatches(tooMany, id -> true));
  }
}
