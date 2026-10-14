package local.luke.power.mining;

import com.google.gson.Gson;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VeinSettingsTest {
  @Test void defaultsAndRulesRoundTripWithRuntimeGson() {
    var gson = new Gson(); var settings = gson.fromJson(gson.toJson(new VeinSettings()), VeinSettings.class);
    settings.validate(); assertFalse(settings.enabled); assertTrue(settings.crouchingOnly && settings.durabilityPerBlock);
    assertTrue(settings.accepts(16,270)); assertFalse(settings.accepts(56,270));
    assertTrue(settings.accepts(74,278)); assertFalse(settings.accepts(1,278));
    assertEquals(2,settings.timeFactor(8));
    assertEquals(1,settings.timeFactor(1)); assertEquals(1,settings.timeFactor(3));
    settings.timePercent = 1; assertEquals(1,settings.timeFactor(64));
    settings.timePercent = 100; assertEquals(128,settings.timeFactor(128));
    settings.tools.put("12", List.of(277)); assertTrue(settings.accepts(12,277)); assertFalse(settings.accepts(12,50));
    settings.tools.put("1", List.of()); assertTrue(settings.accepts(1,0));
  }
  @Test void invalidLimitsAndUnknownRulesCannotBeAccepted() {
    var settings = new VeinSettings(); settings.maxChecks = 16385;
    assertThrows(IllegalArgumentException.class, settings::validate);
    settings.maxChecks = 100; settings.timePercent = 0;
    assertThrows(IllegalArgumentException.class, settings::validate);
    settings.timePercent = 25; settings.tools.put("garbage", List.of(278));
    assertThrows(IllegalArgumentException.class, settings::validate);
    settings.tools = Map.of("16", List.of(999));
    assertThrows(IllegalArgumentException.class, () -> settings.validateTools(id -> id == 16,id -> id == 278));
  }
}
