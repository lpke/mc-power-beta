package local.luke.power.storage;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CurrentConfigFormatTest {
  @Test void retiredAndFutureSchemasRejectWithoutConsumingHistoricalEvidence() {
    for (int version : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 10, 99}) {
      JsonObject source = JsonParser.parseString("{\"schemaVersion\":" + version
          + ",\"settings\":{\"gameplay\":{\"fireSpread\":false}},\"legacyWorldRules\":{\"keep\":true}}").getAsJsonObject();
      JsonObject before = source.deepCopy();
      var error = assertThrows(IllegalArgumentException.class, () -> ConfigVersions.convert(source));
      assertTrue(error.getMessage().contains("Supported version: 9")); assertEquals(before, source);
    }
  }
  @Test void currentSettingsAndOpaqueEvidenceSurviveUnchangedAndIndependent() {
    JsonObject source = JsonParser.parseString("{\"schemaVersion\":9,\"settings\":{\"unknown\":{\"keep\":[1,2]},\"gameRules\":{\"survival\":{\"version\":2,\"values\":{\"keep_inventory\":true}}}},\"legacyWorldRules\":{\"opaque\":true},\"worldRuleUpgrade\":{},\"worldRuleUpgrade210\":{}}").getAsJsonObject();
    JsonObject validated = ConfigVersions.convert(source);
    assertEquals(source, validated);
    validated.getAsJsonObject("settings").addProperty("unrelated", true);
    assertFalse(source.getAsJsonObject("settings").has("unrelated"));
  }
}
