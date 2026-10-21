package local.luke.power.inventory.tools;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class SavedCreativeHotbarsDataTest {
  private String encoded(String data) { return Base64.getEncoder().encodeToString(data.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
  @Test void gson289RoundTripRetainsNineRowsAndFullOpaqueNativeData() {
    var data = SavedCreativeHotbarsData.empty(); String[] bar = new String[9]; bar[0] = encoded("native-NBT;damage=73;count=1000;custom-name=retained");
    data = data.with(8, bar); var root = JsonParser.parseString(new Gson().toJson(data.json())).getAsJsonObject();
    var copy = SavedCreativeHotbarsData.read(root); assertArrayEquals(bar, copy.bar(8));
    assertEquals(9, root.getAsJsonArray("bars").size()); for (var row : root.getAsJsonArray("bars")) assertEquals(9, row.getAsJsonArray().size());
  }
  @Test void saveDraftAndReturnedArraysCannotMutatePreviousOrStoredPresets() {
    var original = SavedCreativeHotbarsData.empty(); String[] bar = new String[9]; bar[0] = encoded("first");
    var changed = original.with(0, bar); bar[0] = encoded("mutated"); assertNull(original.bar(0)[0]); assertEquals(encoded("first"), changed.bar(0)[0]);
    String[] exposed = changed.bar(0); exposed[0] = null; assertEquals(encoded("first"), changed.bar(0)[0]);
  }
  @Test void missingRegistryItemsRemainEncodedAndUnknownCompatibleMetadataIsPreserved() {
    String missing = encoded("item-from-missing-mod"); String[] bar = new String[9]; bar[4] = missing;
    JsonObject root = SavedCreativeHotbarsData.empty().with(2, bar).json(); root.addProperty("userNote", "retain");
    var data = SavedCreativeHotbarsData.read(root).with(0, new String[9]); assertEquals(missing, data.bar(2)[4]); assertEquals("retain", data.json().get("userNote").getAsString());
  }
  @Test void futureFormatAndMalformedShapeAreRejectedWithoutEditingOriginalJson() {
    JsonObject root = SavedCreativeHotbarsData.empty().json(); root.addProperty("format", 2); String before = root.toString();
    assertThrows(IllegalArgumentException.class, () -> SavedCreativeHotbarsData.read(root)); assertEquals(before, root.toString());
    root.addProperty("format", 1); root.getAsJsonArray("bars").remove(8); before = root.toString();
    assertThrows(IllegalArgumentException.class, () -> SavedCreativeHotbarsData.read(root)); assertEquals(before, root.toString());
  }
  @Test void invalidRowsTypesAndPayloadLimitsKeepPriorDataIntact() {
    var data = SavedCreativeHotbarsData.empty(); String original = data.json().toString();
    assertThrows(IllegalArgumentException.class, () -> data.with(-1, new String[9])); assertThrows(IllegalArgumentException.class, () -> data.with(9, new String[9]));
    assertThrows(IllegalArgumentException.class, () -> data.with(0, new String[8]));
    String[] bad = new String[9]; bad[0] = "not base64!"; assertThrows(IllegalArgumentException.class, () -> data.with(0, bad));
    bad[0] = "A".repeat(SavedCreativeHotbarsData.MAX_ITEM + 4); assertThrows(IllegalArgumentException.class, () -> data.with(0, bad)); assertEquals(original, data.json().toString());
  }
  @Test void writingOneRowPreservesEveryOtherRowAndAllowsAnExplicitEmptyPreset() {
    var data = SavedCreativeHotbarsData.empty();
    for (int row = 0; row < 9; row++) { String[] bar = new String[9]; Arrays.fill(bar, encoded("row" + row)); data = data.with(row, bar); }
    var cleared = data.with(4, new String[9]); assertArrayEquals(new String[9], cleared.bar(4));
    for (int row = 0; row < 9; row++) if (row != 4) assertArrayEquals(data.bar(row), cleared.bar(row));
  }
}
