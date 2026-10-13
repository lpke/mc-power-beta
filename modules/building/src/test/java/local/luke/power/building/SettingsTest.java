package local.luke.power.building;

import static org.junit.jupiter.api.Assertions.*;

import local.luke.power.fastplace.*;
import local.luke.power.building.config.*;
import org.junit.jupiter.api.*;

class SettingsTest {
  @Test void retiredSlabMessagesPreserveFeaturePreference() {
    var gson = new com.google.gson.Gson();
    for (boolean enabled : new boolean[]{false, true}) {
      var settings = gson.fromJson("{\"enabled\":" + enabled + ",\"announceToggle\":true}",
          local.luke.power.slabplacement.config.Settings.class);
      assertEquals(enabled, settings.copy().enabled);
      assertFalse(gson.toJsonTree(settings.copy()).getAsJsonObject().has("announceToggle"));
    }
  }
  @Test void inventoryLookDefaultsOffAndSurvivesCopyAndGson() {
    var gson = new com.google.gson.Gson();
    assertFalse(new Settings().inventoryMouseLook);
    assertFalse(gson.fromJson("{}", Settings.class).inventoryMouseLook);
    var settings = new Settings(); settings.inventoryMouseLook = true;
    assertTrue(gson.fromJson(gson.toJson(settings.copy()), Settings.class).inventoryMouseLook);
  }
  @Test void toggleMessagesDefaultOffAndSurviveCopyAndGson() {
    var settings = new Settings();
    assertFalse(settings.sneak.announceToggle);
    assertFalse(settings.placement.announceToggle);
    assertFalse(settings.placement.announceRestrictionToggle);
    assertFalse(settings.autoWalkAnnounceToggle);
    assertFalse(settings.freeLookAnnounceToggle);
    settings.sneak.announceToggle = true;
    settings.placement.announceToggle = settings.placement.announceRestrictionToggle = true;
    settings.autoWalkAnnounceToggle = settings.freeLookAnnounceToggle = true;
    var gson = new com.google.gson.Gson();
    var restored = gson.fromJson(gson.toJson(settings.copy()), Settings.class);
    assertTrue(restored.sneak.announceToggle);
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
