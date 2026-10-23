package local.luke.power.inventory.tools;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class WorldSortingLayoutsDataTest {
  private static final String FIRST = "00000000-0000-0000-0000-000000000001", SECOND = "00000000-0000-0000-0000-000000000002";
  private JsonObject legacy() { return JsonParser.parseString("{\"inventoryTools\":{\"protectedSlots\":[9],\"preferredItems\":{\"10\":\"35:4\"},\"restock\":true},\"sortingLayouts\":{\"format\":999,\"future\":[1,2]}}").getAsJsonObject(); }
  @Test void migrationFreezesLegacyPresetsAndCopiesIndependentWorldLayouts() {
    var source = legacy(); String before = source.toString();
    var data = WorldSortingLayoutsData.migrate(source).seed(List.of(FIRST));
    var edited = data.layout(FIRST); edited.getAsJsonArray("protectedSlots").add(12); edited.addProperty("opaque", "keep");
    data = data.replace(FIRST, edited).seed(List.of(SECOND));
    assertEquals(before, source.toString());
    assertEquals(2, data.layout(FIRST).getAsJsonArray("protectedSlots").size());
    assertEquals(1, data.layout(SECOND).getAsJsonArray("protectedSlots").size());
    assertEquals(999, data.layout(SECOND).getAsJsonObject("presets").get("format").getAsInt());
    assertEquals("keep", data.layout(FIRST).get("opaque").getAsString());
    var restarted = WorldSortingLayoutsData.read(JsonParser.parseString(new Gson().toJson(data.json())).getAsJsonObject());
    assertEquals(data.json(), restarted.json());
  }
  @Test void existingWorldWinsOverLegacyAndFutureEntriesSurviveOtherWorldEdits() {
    var data = WorldSortingLayoutsData.migrate(legacy()).seed(List.of(FIRST));
    var root = data.json(); root.addProperty("extension", true);
    JsonObject unknown = new JsonObject(); unknown.addProperty("future", 9); root.getAsJsonObject("worlds").add(SECOND, unknown);
    data = WorldSortingLayoutsData.read(root).seed(List.of(FIRST, SECOND));
    assertEquals(unknown, data.json().getAsJsonObject("worlds").get(SECOND));
    assertThrows(RuntimeException.class, () -> WorldSortingLayoutsData.read(root).layout(SECOND));
    assertTrue(data.json().get("extension").getAsBoolean());
  }
  @Test void futureRootAndInvalidIdentityRejectWithoutChangingOriginalBytes() {
    var root = WorldSortingLayoutsData.migrate(legacy()).json(); root.addProperty("format", 2); String before = root.toString();
    assertThrows(IllegalArgumentException.class, () -> WorldSortingLayoutsData.read(root)); assertEquals(before, root.toString());
    assertThrows(IllegalArgumentException.class, () -> WorldSortingLayoutsData.migrate(legacy()).seed(List.of("folder-name")));
  }
}
