package local.luke.creative;

import static org.junit.jupiter.api.Assertions.*;

import local.luke.creative.flight.FlightSprint;
import org.junit.jupiter.api.Test;

class FlightSprintTest {
  @Test
  void toggleSurvivesReleaseAndStopsOnNextPress() {
    var s = new FlightSprint();
    assertTrue(s.update(true, true, true));
    assertTrue(s.update(true, true, true));
    assertTrue(s.update(false, true, true));
    assertFalse(s.update(true, true, true));
    assertFalse(s.update(false, true, true));
  }

  @Test
  void holdTracksTheKeyWithoutLatching() {
    var s = new FlightSprint();
    assertTrue(s.update(true, true, false));
    assertFalse(s.update(false, true, false));
  }

  @Test
  void FocusOrFlightLossRequiresFreshPress() {
    var s = new FlightSprint();
    assertTrue(s.update(true, true, true));
    assertFalse(s.update(true, false, true));
    assertFalse(s.update(true, true, true));
    assertFalse(s.update(false, true, true));
    assertTrue(s.update(true, true, true));
  }

  @Test
  void resetAndModeChangeDoNotCarryTheLatch() {
    var s = new FlightSprint();
    s.update(true, true, true);
    s.reset(true);
    assertFalse(s.update(true, true, true));
    assertTrue(s.update(true, true, false));
    assertFalse(s.update(true, true, true));
    assertFalse(s.update(false, true, true));
    assertTrue(s.update(true, true, true));
  }
}
