package local.luke.power.building;

import static org.junit.jupiter.api.Assertions.*;

import local.luke.power.fastplace.*;
import local.luke.power.building.config.*;
import org.junit.jupiter.api.*;

class SettingsTest {
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
