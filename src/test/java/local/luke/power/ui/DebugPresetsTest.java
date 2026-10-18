package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import org.junit.jupiter.api.Test;

class DebugPresetsTest {
  private JsonObject preset() {
    return JsonParser.parseString("{\"id\":\"stable\",\"name\":\"My layout\",\"left\":[\"version\",\"\",\"future.provider\"],\"right\":[\"usedMemory\"],\"values\":{\"interface.debugLines.version\":true}}").getAsJsonObject();
  }
  @Test void gson289RoundTripPreservesUnknownRowsAndGaps() {
    JsonArray saved = new JsonArray(); saved.add(preset());
    JsonArray result = DebugPresets.checked(new Gson().fromJson(new Gson().toJson(saved), JsonArray.class));
    assertEquals(saved, result);
    saved.get(0).getAsJsonObject().addProperty("name", "Changed");
    assertEquals("My layout", result.get(0).getAsJsonObject().get("name").getAsString());
  }
  @Test void rejectsDuplicatesAndNonF3SettingPayloads() {
    JsonArray saved = new JsonArray(); saved.add(preset()); saved.add(preset());
    assertThrows(IllegalArgumentException.class, () -> DebugPresets.checked(saved));
    saved.remove(1); saved.get(0).getAsJsonObject().getAsJsonObject("values").addProperty("creative.flight", true);
    assertThrows(IllegalArgumentException.class, () -> DebugPresets.checked(saved));
  }
}
