package local.luke.power.creative;

import static org.junit.jupiter.api.Assertions.*;

import local.luke.power.creative.flight.FlightSprint;
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
  void forwardReleaseEndsOnlyTheCurrentSprint() {
    var s = new FlightSprint();
    assertTrue(s.update(true, true, true));
    assertTrue(s.update(false, true, true));
    assertFalse(s.update(false, false, true));
    assertFalse(s.update(false, true, true));
    assertTrue(s.update(true, true, true));
    assertFalse(s.update(true, false, true));
    assertFalse(s.update(true, true, true));
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
  @Test void takeoffInheritsToggleSprintWithAnAlreadyHeldKeyAndConsumesItOnce() {
    var s = new FlightSprint();
    s.update(true, false, true);
    s.takeoff(true, true);
    assertTrue(s.update(true, true, true));
    assertTrue(s.update(true, true, true));
    assertTrue(s.update(false, true, true));
    assertFalse(s.update(true, true, true));
    assertFalse(s.update(false, true, true));
  }

  @Test void releasedGroundToggleCarriesIntoFlightToggleButHoldStillRequiresTheKey() {
    var s = new FlightSprint();
    s.takeoff(true, false);
    assertTrue(s.update(false, true, true));
    assertTrue(s.update(false, true, true));
    s.takeoff(true, false);
    assertFalse(s.update(false, true, false));
    s.takeoff(true, true);
    assertTrue(s.update(true, true, false));
    assertFalse(s.update(false, true, false));
  }

  @Test void normalSpeedGroundSprintTransfersThroughItsPublishedActiveFlag() {
    local.luke.power.input.SprintView.publish(local.luke.power.input.SprintView.Kind.GROUND, true, 1);
    try {
      var s = new FlightSprint();
      s.takeoff(local.luke.power.input.SprintView.active(local.luke.power.input.SprintView.Kind.GROUND), false);
      assertTrue(s.update(false, true, true));
    } finally {
      local.luke.power.input.SprintView.publish(local.luke.power.input.SprintView.Kind.GROUND, false, 1);
    }
  }

  @Test void deniedOrCanceledFlightCannotKeepPendingGroundSprint() {
    var s = new FlightSprint();
    s.takeoff(true, true);
    assertFalse(s.update(true, false, true));
    assertFalse(s.update(true, true, true));
    assertFalse(s.update(false, true, true));
    assertTrue(s.update(true, true, true));
    s.takeoff(false, true);
    assertFalse(s.update(true, true, true));
  }

  @Test void focusMenuWorldAndMovementResetsDiscardPendingTakeoff() {
    var s = new FlightSprint();
    s.takeoff(true, true);
    s.reset(true);
    assertFalse(s.update(true, true, true));
    s.takeoff(true, false);
    assertTrue(s.update(false, true, true));
    assertFalse(s.update(false, false, true));
    assertFalse(s.update(false, true, true));
  }
  @Test void aNewPressAfterTakeoffCanTurnOffTheInheritedToggleImmediately() {
    var s = new FlightSprint();
    s.takeoff(true, false);
    assertFalse(s.update(true, true, true));
    assertFalse(s.update(false, true, true));
    assertTrue(s.update(true, true, true));
  }
}
