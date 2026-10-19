package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.Gson;
import com.google.gson.JsonPrimitive;
import java.util.List;
import local.luke.power.config.DefaultBases;
import org.junit.jupiter.api.Test;

class PauseToolsPreferencesTest {
  @Test void positionsAndIncludedButtonsRoundTripWithoutResettingOtherPreferences() {
    var gson = new Gson();
    var values = gson.fromJson("{\"pauseToolsPosition\":8,\"pauseMapButton\":false,\"pauseWaypointsButton\":true,\"debugTextSize\":2}", MenuPreferences.Values.class);
    values.validate();
    var restored = gson.fromJson(gson.toJson(values), MenuPreferences.Values.class);
    assertEquals(8, restored.pauseToolsPosition); assertFalse(restored.pauseMapButton);
    assertTrue(restored.pauseWaypointsButton); assertEquals(2, restored.debugTextSize);
    for (int invalid : new int[]{-1, 9}) {
      restored.pauseToolsPosition = invalid; assertThrows(IllegalArgumentException.class, restored::validate);
    }
  }
  @Test void allThreeButtonsDefaultOnOnlyForPowerBetaAndKeepTheLegacyPhotoId() {
    var fallback = new MenuPreferences.Values();
    assertEquals(0, fallback.pauseToolsPosition); assertTrue(fallback.pauseMapButton); assertTrue(fallback.pauseWaypointsButton);
    for (var base : DefaultBases.Base.values()) {
      assertEquals(0, DefaultBases.value(base, "interface.pauseToolsPosition", new JsonPrimitive(0)).getAsInt());
      for (String id : List.of("interface.pauseMapButton", "interface.pauseWaypointsButton", "power_controls:userinterface.photoModeConfig.enablePhotoModeButton"))
        assertEquals(base == DefaultBases.Base.POWER_BETA, DefaultBases.value(base, id, new JsonPrimitive(true)).getAsBoolean(), base + " " + id);
    }
  }
}
