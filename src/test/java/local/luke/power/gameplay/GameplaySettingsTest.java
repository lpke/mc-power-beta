package local.luke.power.gameplay;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

class GameplaySettingsTest {
  @Test void defaultsPreserveGameplayAndMissingFieldsUseSafeLimits() {
    var gson = new Gson(); var settings = gson.fromJson("{}", GameplaySettings.class);
    assertTrue(settings.mergeItems); assertFalse(settings.limitTnt);
    assertTrue(settings.flintIgnitesTnt);
    assertTrue(settings.leavesDropSaplingsInCreative);
    assertEquals(256, settings.mergeThreshold); assertEquals(4, settings.tntPerTick); assertEquals(8, settings.tntMillis);
    settings.mergeItems = false; settings.limitTnt = true;
    settings.flintIgnitesTnt = false;
    var loaded = gson.fromJson(gson.toJson(settings), GameplaySettings.class);
    assertFalse(loaded.mergeItems); assertTrue(loaded.limitTnt); loaded.validate();
    assertFalse(loaded.flintIgnitesTnt);
    loaded.tntPerTick = 0; assertThrows(IllegalArgumentException.class, loaded::validate);
  }
}
