package local.luke.power.storage;

import com.google.gson.*;
import local.luke.power.ui.DebugOrder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DebugConfigUpgradeTest {
  @Test void retiredOffsetsRemainOpaqueWithoutReplacingCurrentLayoutDefaults() {
    JsonObject source = JsonParser.parseString("{\"schemaVersion\":8,\"settings\":{\"interface\":{},\"power_compat:config\":{\"overlayAdditionsYOffset\":80},\"entityculling:config\":{\"f3InfoYOffset\":-90}}}").getAsJsonObject();
    assertEquals(source, ConfigVersions.convert(source));
    assertFalse(ConfigVersions.convert(source).getAsJsonObject("settings").getAsJsonObject("interface").has("debugOrder"));
  }
  @Test void invalidLayoutsBlockConversionAndFutureIdsArePreserved() {
    for (String body : new String[]{"\"debugOrder\":[1]", "\"debugOrder\":[\"x\",\"x\"]", "\"debugTextSize\":0", "\"debugRightOrder\":[1]", "\"debugOrder\":[\"version\"],\"debugRightOrder\":[\"version\"]", "\"debugLines\":{\"x\":1}"}) {
      var source = JsonParser.parseString("{\"schemaVersion\":8,\"settings\":{\"interface\":{" + body + "}}}").getAsJsonObject();
      var before = source.deepCopy(); assertThrows(IllegalArgumentException.class, () -> ConfigVersions.convert(source)); assertEquals(before, source);
    }
    var future = JsonParser.parseString("{\"schemaVersion\":8,\"settings\":{\"interface\":{\"debugOrder\":[\"future.provider\",\"\",\"x\"],\"debugLines\":{\"future.provider\":false}}}}").getAsJsonObject();
    assertEquals(future, ConfigVersions.convert(future));
  }
}
