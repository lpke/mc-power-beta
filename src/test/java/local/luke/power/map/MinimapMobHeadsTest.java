package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

class MinimapMobHeadsTest {
  @Test void middleClicksToggleUntilAnotherClickOrAnAcceptedHoldPress() {
    var heads = new MinimapMobHeads();
    assertFalse(heads.shown());
    heads.toggle(); assertTrue(heads.shown()); assertFalse(heads.held());
    heads.sample(false, true); assertTrue(heads.shown());
    heads.toggle(); assertFalse(heads.shown());
    heads.toggle(); heads.press();
    assertTrue(heads.held()); assertFalse(heads.toggled());
    heads.sample(false, true); assertFalse(heads.shown());
  }

  @Test void tickSamplingCatchesHeldBindingsWithoutReclearingDuringAnExistingHold() {
    var heads = new MinimapMobHeads();
    heads.toggle(); heads.sample(true, true);
    assertTrue(heads.held()); assertFalse(heads.toggled());
    heads.toggle(); heads.sample(true, true);
    assertTrue(heads.toggled());
    heads.sample(false, true); assertTrue(heads.shown());
    heads.sample(true, true); heads.sample(false, true);
    assertFalse(heads.shown());
  }

  @Test void idleSamplingPreservesTheLatchAndWorldExitClearsBothInputs() {
    var heads = new MinimapMobHeads(); heads.toggle();
    for (int ignored = 0; ignored < 4; ignored++) heads.sample(false, true);
    assertTrue(heads.shown());
    heads.clear(); assertFalse(heads.shown()); assertFalse(heads.held());
    heads.press(); heads.clear(); assertFalse(heads.shown());
  }

  @Test void menuFocusAndAvailabilityChangesDoNotTurnAnExistingHoldIntoANewPress() {
    var heads = new MinimapMobHeads(); heads.press(); heads.toggle();
    heads.sample(true, false);
    assertFalse(heads.held()); assertTrue(heads.toggled());
    heads.sample(true, true);
    assertTrue(heads.held()); assertTrue(heads.toggled());
    heads.sample(false, false); heads.sample(true, false);
    heads.sample(true, true);
    assertTrue(heads.toggled(), "A press owned by another context must stay owned there");
    heads.sample(false, true); heads.sample(true, true);
    assertTrue(heads.held()); assertFalse(heads.toggled());
  }

  @Test void sessionActionsNeverChangePersistedMapPreferencesOrLegacyActionIds() {
    var gson = new Gson(); var settings = new MapSettings();
    settings.middleClickAction = 3; settings.radar = false; settings.worldMapRadarIcons = true;
    String before = gson.toJson(settings);
    var heads = new MinimapMobHeads(); heads.toggle(); heads.press(); heads.sample(false, true); heads.clear();
    assertEquals(before, gson.toJson(settings));
    for (int action = 0; action <= 5; action++) {
      var restored = gson.fromJson("{\"middleClickAction\":" + action + "}", MapSettings.class);
      assertEquals(action, restored.middleClickAction);
      assertDoesNotThrow(restored::validate);
    }
    for (var base : local.luke.power.config.DefaultBases.Base.values())
      assertEquals(5, local.luke.power.config.DefaultBases.value(base, "maps.middleClickAction",
          new com.google.gson.JsonPrimitive(5)).getAsInt());
  }
}
