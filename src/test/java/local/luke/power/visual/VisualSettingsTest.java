package local.luke.power.visual;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

class VisualSettingsTest {
  @Test void existingConfigsKeepVanillaWaterFogAndPercentagesRoundTrip() {
    var gson = new Gson(); // The game's Gson 2.8.9.
    var settings = gson.fromJson("{\"removeUnderwaterOverlay\":true}", VisualSettings.class);
    assertEquals(100, settings.underwaterFogDensity);
    assertEquals(100, settings.underwaterDarkness);
    for (int percentage : new int[]{0, 1, 25, 50, 75, 100}) {
      settings.underwaterFogDensity = percentage;
      settings.underwaterDarkness = 100 - percentage;
      settings.validate();
      var restored = gson.fromJson(gson.toJson(settings), VisualSettings.class);
      assertEquals(percentage, restored.underwaterFogDensity);
      assertEquals(100 - percentage, restored.underwaterDarkness);
      assertTrue(restored.removeUnderwaterOverlay);
      assertFalse(restored.spectatorFullBright);
    }
  }

  @Test void waterFogRejectsInvalidPercentages() {
    var settings = new VisualSettings();
    for (int invalid : new int[]{-1, 101, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
      settings.underwaterFogDensity = invalid;
      assertThrows(IllegalArgumentException.class, settings::validate);
    }
  }

  @Test void waterDarknessRejectsInvalidPercentages() {
    var settings = new VisualSettings();
    for (int invalid : new int[]{-1, 101, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
      settings.underwaterDarkness = invalid;
      assertThrows(IllegalArgumentException.class, settings::validate);
    }
  }
}
