package local.luke.power.building;

import static org.junit.jupiter.api.Assertions.*;

import local.luke.power.fastplace.*;
import local.luke.power.building.config.*;
import org.junit.jupiter.api.*;

class SettingsTest {
  @Test void toggleMessagesDefaultOffAndSurviveCopyAndGson() {
    var settings = new Settings();
    assertFalse(settings.sneak.announceToggle);
    assertFalse(settings.slabs.announceToggle);
    assertFalse(settings.placement.announceToggle);
    assertFalse(settings.placement.announceRestrictionToggle);
    assertFalse(settings.autoWalkAnnounceToggle);
    assertFalse(settings.freeLookAnnounceToggle);
    settings.sneak.announceToggle = settings.slabs.announceToggle = true;
    settings.placement.announceToggle = settings.placement.announceRestrictionToggle = true;
    settings.autoWalkAnnounceToggle = settings.freeLookAnnounceToggle = true;
    var gson = new com.google.gson.Gson();
    var restored = gson.fromJson(gson.toJson(settings.copy()), Settings.class);
    assertTrue(restored.sneak.announceToggle && restored.slabs.announceToggle);
    assertTrue(restored.placement.announceToggle && restored.placement.announceRestrictionToggle);
    assertTrue(restored.autoWalkAnnounceToggle && restored.freeLookAnnounceToggle);
  }

  @Test
  void slabModesMatchTheStartingActionAndLayer() {
    assertTrue(SlabPolicy.allows(SlabMode.CONTINUOUS, false, true, 64, 64));
    assertTrue(SlabPolicy.allows(SlabMode.CONTINUOUS, true, false, 64, 65));
    assertTrue(SlabPolicy.allows(SlabMode.DOUBLE, false, false, 64, 65));
    assertFalse(SlabPolicy.allows(SlabMode.MATCH_FIRST, false, true, 64, 64));
    assertFalse(SlabPolicy.allows(SlabMode.MATCH_FIRST, true, false, 64, 64));
    assertTrue(SlabPolicy.allows(SlabMode.MATCH_FIRST, true, true, 64, 64));
    assertFalse(SlabPolicy.allows(SlabMode.MATCH_FIRST, false, false, 64, 65));
  }
}
