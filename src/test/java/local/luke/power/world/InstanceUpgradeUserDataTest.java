package local.luke.power.world;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import org.junit.jupiter.api.Test;

class InstanceUpgradeUserDataTest {
  @Test void defaultsMaintenanceKeepsOpaqueSortingAndHotbarDocumentsWithoutSharingMutableData() {
    var previous = JsonParser.parseString("{\"sortingLayouts\":{\"format\":999,\"opaque\":[1,\"future\"]},\"savedCreativeHotbars\":{\"format\":999,\"future\":true}}").getAsJsonObject();
    var fresh = new JsonObject();
    InstanceUpgrade.preserveSavedData(previous, fresh);
    assertEquals(previous.get("sortingLayouts"), fresh.get("sortingLayouts"));
    assertEquals(previous.get("savedCreativeHotbars"), fresh.get("savedCreativeHotbars"));
    assertEquals(previous.get("sortingLayouts"), fresh.getAsJsonObject("worldSortingLayouts").getAsJsonObject("legacy").get("presets"));
    fresh.getAsJsonObject("sortingLayouts").addProperty("changed", true);
    assertFalse(previous.getAsJsonObject("sortingLayouts").has("changed"));
  }
  @Test void freshDefaultsNeverInventSavedUserData() {
    var fresh = new JsonObject(); InstanceUpgrade.preserveSavedData(new JsonObject(), fresh);
    assertFalse(fresh.has("sortingLayouts")); assertFalse(fresh.has("savedCreativeHotbars"));
  }
}
