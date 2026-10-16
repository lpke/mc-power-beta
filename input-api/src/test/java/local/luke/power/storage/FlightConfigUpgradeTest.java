package local.luke.power.storage;

import com.google.gson.*;
import local.luke.power.input.FlightMotion;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightConfigUpgradeTest {
  static JsonObject legacy() {
    return JsonParser.parseString("{\"schemaVersion\":6,\"worldRuleUpgrade210\":{\"unchanged\":true},\"settings\":{\"creative\":{\"sprintFlight\":false,\"spectatorSprintFlight\":false,\"flightSpeed\":137},\"power_camera:config\":{\"speed\":12.375,\"drag\":10,\"sprint\":false,\"classicMovement\":false,\"collision\":true,\"unknown\":\"keep\"}}}").getAsJsonObject();
  }
  @Test void convertsCoastingOnceAndPreservesSpeedAndUnrelatedData() {
    JsonObject old = legacy(), original = old.deepCopy(), converted = ConfigVersions.convert(old);
    assertEquals(original, old);
    var camera = converted.getAsJsonObject("settings").getAsJsonObject("power_camera:config");
    assertEquals(12.375, camera.get("speed").getAsDouble());
    assertEquals(FlightMotion.legacyDrift(10), camera.get("drag").getAsDouble(), .000001);
    assertFalse(camera.has("sprint")); assertFalse(camera.has("classicMovement"));
    assertEquals("keep", camera.get("unknown").getAsString()); assertTrue(camera.get("collision").getAsBoolean());
    var creative = converted.getAsJsonObject("settings").getAsJsonObject("creative");
    assertFalse(creative.has("sprintFlight")); assertFalse(creative.has("spectatorSprintFlight"));
    assertEquals(137, creative.get("flightSpeed").getAsInt());
    assertEquals(old.get("worldRuleUpgrade210"),converted.get("worldRuleUpgrade210"));
    assertEquals(converted, ConfigVersions.convert(new Gson().fromJson(converted.toString(),JsonObject.class)));
    old.getAsJsonObject("settings").getAsJsonObject("power_camera:config").addProperty("classicMovement",true);
    assertEquals(0, ConfigVersions.convert(old).getAsJsonObject("settings").getAsJsonObject("power_camera:config").get("drag").getAsInt());
  }
}
