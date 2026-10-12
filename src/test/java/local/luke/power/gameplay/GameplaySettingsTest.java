package local.luke.power.gameplay;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

class GameplaySettingsTest {
  @Test void defaultsPreserveGameplayAndMissingFieldsUseSafeLimits() {
    var gson = new Gson(); var settings = gson.fromJson("{}", GameplaySettings.class);
    assertFalse(settings.mergeItems); assertFalse(settings.explosionProofItems); assertFalse(settings.limitTnt);
    assertTrue(settings.leavesDropSaplingsInCreative);
    assertEquals(256, settings.mergeThreshold); assertEquals(4, settings.tntPerTick); assertEquals(8, settings.tntMillis);
    settings.mergeItems = settings.explosionProofItems = settings.limitTnt = true;
    var loaded = gson.fromJson(gson.toJson(settings), GameplaySettings.class);
    assertTrue(loaded.mergeItems && loaded.explosionProofItems && loaded.limitTnt); loaded.validate();
    loaded.tntPerTick = 0; assertThrows(IllegalArgumentException.class, loaded::validate);
  }
}
