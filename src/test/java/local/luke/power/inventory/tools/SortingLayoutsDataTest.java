package local.luke.power.inventory.tools;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import org.junit.jupiter.api.Test;

class SortingLayoutsDataTest {
  private final JsonArray locked = JsonParser.parseString("[9,35]").getAsJsonArray();
  private final JsonObject preferred = JsonParser.parseString("{\"10\":\"999999:17\"}").getAsJsonObject();
  @Test void gson289RoundTripRetainsMissingItemsAndIndependentDrafts() {
    var empty = SortingLayoutsData.read(new JsonObject()); var saved = empty.save("Mining", locked, preferred);
    var copy = SortingLayoutsData.read(JsonParser.parseString(new Gson().toJson(saved.json())).getAsJsonObject());
    assertEquals(0, empty.size()); assertEquals(saved.json(), copy.json());
    locked.add(11); preferred.addProperty("11", "1");
    assertEquals(2, copy.preset(0).getAsJsonArray("protectedSlots").size()); assertFalse(copy.preset(0).getAsJsonObject("preferredItems").has("11"));
    var exposed = copy.preset(0); exposed.addProperty("name", "Changed"); assertEquals("Mining", copy.preset(0).get("name").getAsString());
  }
  @Test void deletionKeepsOtherStableIdsAndUnknownCompatibleFields() {
    var saved = SortingLayoutsData.read(new JsonObject()).save("First", locked, preferred).save("Second", locked, preferred);
    JsonObject root = saved.json(); root.addProperty("note", "keep"); root.getAsJsonArray("presets").get(1).getAsJsonObject().addProperty("extra", "keep too");
    var removed = SortingLayoutsData.read(root).delete(0);
    assertEquals("keep", removed.json().get("note").getAsString());
    assertEquals(root.getAsJsonArray("presets").get(1), removed.preset(0)); assertEquals(2, saved.size());
  }
  @Test void futureFormatsAndInvalidSlotsRejectWithoutTouchingOriginal() {
    JsonObject root = SortingLayoutsData.read(new JsonObject()).save("Saved", locked, preferred).json(); root.addProperty("format", 2);
    String before = root.toString(); assertThrows(IllegalArgumentException.class, () -> SortingLayoutsData.read(root)); assertEquals(before, root.toString());
    for (String slots : new String[]{"[8]", "[36]", "[9,9]", "[9.5]", "[\"9\"]"})
      assertThrows(RuntimeException.class, () -> SortingLayoutsData.layout(JsonParser.parseString(slots), preferred));
    assertThrows(IllegalArgumentException.class, () -> SortingLayoutsData.layout(locked, JsonParser.parseString("{\"36\":\"1\"}")));
  }
  @Test void invalidNamesAndFullLibraryPreservePriorSnapshot() {
    var saved = SortingLayoutsData.read(new JsonObject());
    assertThrows(IllegalArgumentException.class, () -> SortingLayoutsData.read(new JsonObject()).save(" ", locked, preferred));
    for (int n = 0; n < SortingLayoutsData.LIMIT; n++) saved = saved.save("Layout " + n, locked, preferred);
    var full = saved; JsonObject before = full.json();
    assertThrows(IllegalArgumentException.class, () -> full.save("Too many", locked, preferred)); assertEquals(before, full.json());
  }
}
