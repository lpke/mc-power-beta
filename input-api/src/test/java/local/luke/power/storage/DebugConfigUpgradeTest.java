package local.luke.power.storage;

import com.google.gson.*;
import local.luke.power.ui.DebugOrder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DebugConfigUpgradeTest {
  @Test void legacyOffsetsBecomeStableRowsOnceWithoutChangingTheOldEvidence() {
    JsonObject source = JsonParser.parseString("{\"schemaVersion\":7,\"settings\":{\"interface\":{},\"power_compat:config\":{\"overlayAdditionsYOffset\":80},\"entityculling:config\":{\"f3InfoYOffset\":-90}}}").getAsJsonObject();
    var converted = ConfigVersions.convert(source);
    var order = DebugOrder.read(converted.getAsJsonObject("settings").getAsJsonObject("interface").get("debugOrder"));
    assertTrue(order.indexOf("cullingTime") < order.indexOf("biome"));
    assertEquals(80, converted.getAsJsonObject("settings").getAsJsonObject("power_compat:config").get("overlayAdditionsYOffset").getAsInt());
    assertFalse(source.getAsJsonObject("settings").getAsJsonObject("interface").has("debugOrder"));
    converted.getAsJsonObject("settings").getAsJsonObject("power_compat:config").addProperty("overlayAdditionsYOffset", -80);
    assertEquals(order, DebugOrder.read(ConfigVersions.convert(converted).getAsJsonObject("settings").getAsJsonObject("interface").get("debugOrder")));
  }
  @Test void invalidLayoutsBlockConversionAndFutureIdsArePreserved() {
    for (String body : new String[]{"\"debugOrder\":[1]", "\"debugOrder\":[\"x\",\"x\"]", "\"debugTextSize\":0", "\"debugRightOrder\":[1]", "\"debugOrder\":[\"version\"],\"debugRightOrder\":[\"version\"]", "\"debugLines\":{\"x\":1}"}) {
      var source = JsonParser.parseString("{\"schemaVersion\":7,\"settings\":{\"interface\":{" + body + "}}}").getAsJsonObject();
      var before = source.deepCopy(); assertThrows(IllegalArgumentException.class, () -> ConfigVersions.convert(source)); assertEquals(before, source);
    }
    var future = JsonParser.parseString("{\"schemaVersion\":7,\"settings\":{\"interface\":{\"debugOrder\":[\"future.provider\",\"\",\"x\"],\"debugLines\":{\"future.provider\":false}}}}").getAsJsonObject();
    assertEquals(future, ConfigVersions.convert(future));
  }
}
