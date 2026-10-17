package local.luke.power.gameplay;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

class GameplaySettingsTest {
  @Test void defaultsPreserveGameplayAndMissingFieldsUseSafeLimits() {
    var gson = new Gson(); var settings = gson.fromJson("{}", GameplaySettings.class);
    assertTrue(settings.mergeItems); assertTrue(settings.limitTnt);
    assertTrue(settings.flintIgnitesTnt);
    assertFalse(settings.axesSlashLeaves);
    assertTrue(settings.leavesDropSaplingsInCreative);
    assertEquals(256, settings.mergeThreshold); assertEquals(4, settings.tntPerTick); assertEquals(8, settings.tntMillis);
    settings.mergeItems = false; settings.limitTnt = false;
    settings.flintIgnitesTnt = false;
    settings.axesSlashLeaves = true;
    var loaded = gson.fromJson(gson.toJson(settings), GameplaySettings.class);
    assertFalse(loaded.mergeItems); assertFalse(loaded.limitTnt); loaded.validate();
    assertFalse(loaded.flintIgnitesTnt);
    assertTrue(loaded.axesSlashLeaves);
    loaded.tntPerTick = 0; assertThrows(IllegalArgumentException.class, loaded::validate);
  }
}
