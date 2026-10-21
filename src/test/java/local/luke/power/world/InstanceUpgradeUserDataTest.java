package local.luke.power.world;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import org.junit.jupiter.api.Test;

class InstanceUpgradeUserDataTest {
  @Test void defaultsMaintenanceKeepsOpaqueSortingAndHotbarDocumentsWithoutSharingMutableData() {
    var previous = JsonParser.parseString("{\"sortingLayouts\":{\"format\":999,\"opaque\":[1,\"future\"]},\"savedCreativeHotbars\":{\"format\":999,\"future\":true}}").getAsJsonObject();
    var fresh = new JsonObject();
    InstanceUpgrade.preserveSavedData(previous, fresh);
    assertEquals(previous, fresh);
    fresh.getAsJsonObject("sortingLayouts").addProperty("changed", true);
    assertFalse(previous.getAsJsonObject("sortingLayouts").has("changed"));
  }
  @Test void freshDefaultsNeverInventSavedUserData() {
    var fresh = new JsonObject(); InstanceUpgrade.preserveSavedData(new JsonObject(), fresh);
    assertFalse(fresh.has("sortingLayouts")); assertFalse(fresh.has("savedCreativeHotbars"));
  }
}
