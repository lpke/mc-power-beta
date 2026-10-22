package local.luke.power.autowalk;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class WalkToggleTest {
  @Test void focusLossPreservesDirectionAndDoesNotTreatReleasedKeysAsHoldCancellation() {
    WalkToggle t = new WalkToggle();
    t.update(true, true, true, 0, 350, true);
    t.unfocused();
    t.update(true, true, true, 1_000_000_000L, 350, false);
    assertEquals(-1, t.input());
    t.update(false, true, true, 2_000_000_000L, 350, false);
    assertEquals(-1, t.input());
    t.update(true, true, true, 3_000_000_000L, 350, false);
    assertFalse(t.isActive());
  }
  @Test void backwardsDirectionLatchesUntilStoppedAndPreservesManualAndSneakInput() {
    WalkToggle t = new WalkToggle();
    t.update(true, true, false, 0, 350, true);
    assertEquals(-1F, t.input());
    assertEquals(-1F, t.movementInput(-1F, false, true, false));
    t.update(false, true, false, 10, 350, false);
    assertEquals(-1F, t.movementInput(0, false, false, false));
    assertEquals(-0.3F, t.movementInput(0, false, false, true));
    assertEquals(0F, t.movementInput(1F, true, false, false));
    t.stop();
    assertEquals(0F, t.input());
    t.update(true, true, false, 20, 350, false);
    assertEquals(1F, t.input());
  }

  @Test void backwardsHoldKeepsItsDirectionAndStopsOnReleaseOrFocusLoss() {
    WalkToggle t = new WalkToggle();
    t.update(true, true, true, 0, 350, true);
    t.update(true, true, true, 400_000_000L, 350, false);
    assertEquals(-1F, t.input());
    t.update(false, true, true, 450_000_000L, 350, false);
    assertEquals(0F, t.input());
    t.update(true, true, true, 500_000_000L, 350, true);
    t.update(true, false, true, 900_000_000L, 350, true);
    t.update(true, true, true, 1_000_000_000L, 350, true);
    assertEquals(0F, t.input());
  }
  @Test
  void shortTapTogglesButLongHoldStopsOnRelease() {
    WalkToggle t = new WalkToggle();
    t.update(true, true, true, 0, 350); assertTrue(t.isActive());
    t.update(false, true, true, 100_000_000L, 350); assertTrue(t.isActive());
    t.update(true, true, true, 200_000_000L, 350); assertFalse(t.isActive());
    t.update(true, true, true, 600_000_000L, 350); assertTrue(t.isActive());
    t.update(false, true, true, 650_000_000L, 350); assertFalse(t.isActive());
  }

  @Test
  void holdReleaseWithoutIntermediateTicksAndMenuOrManualStopAreSafe() {
    WalkToggle t = new WalkToggle();
    t.update(true, true, true, 0, 350);
    t.update(false, true, true, 500_000_000L, 350); assertFalse(t.isActive());
    t.update(true, true, true, 600_000_000L, 350);
    t.stop(); t.update(true, true, true, 1_200_000_000L, 350); assertFalse(t.isActive());
    t.update(false, true, true, 1_300_000_000L, 350);
    t.update(true, false, true, 1_400_000_000L, 350);
    t.update(true, true, true, 2_000_000_000L, 350); assertFalse(t.isActive());
  }

  @Test
  void pressTogglesOnceAndReleaseDoesNotStopWalking() {
    WalkToggle toggle = new WalkToggle();
    toggle.update(true, true);
    assertTrue(toggle.isActive());
    for (int i = 0; i < 100; i++) toggle.update(true, true);
    assertTrue(toggle.isActive());
    toggle.update(false, true);
    assertTrue(toggle.isActive());
    toggle.update(true, true);
    assertFalse(toggle.isActive());
  }

  @Test
  void menuOrLostFocusStopsAndHeldKeyCannotRestartIt() {
    WalkToggle toggle = new WalkToggle();
    toggle.update(true, true);
    toggle.update(true, false);
    assertFalse(toggle.isActive());
    toggle.update(true, true);
    assertFalse(toggle.isActive());
    toggle.update(false, true);
    toggle.update(true, true);
    assertTrue(toggle.isActive());
  }

  @Test
  void pressingInsideMenuDoesNotStartWalkingOnReturn() {
    WalkToggle toggle = new WalkToggle();
    toggle.update(true, false);
    toggle.update(true, true);
    assertFalse(toggle.isActive());
  }

  @Test
  void worldChangeAndScreenOpenCanStopImmediately() {
    WalkToggle toggle = new WalkToggle();
    toggle.update(true, true);
    toggle.stop();
    assertFalse(toggle.isActive());
    toggle.update(true, true);
    assertFalse(toggle.isActive());
  }

  @Test
  void movementPreservesVanillaSpeedSneakingAndOpposingKeys() {
    assertEquals(1F, WalkToggle.forwardInput(0F, false, false));
    assertEquals(1F, WalkToggle.forwardInput(1F, true, false));
    assertEquals(0.3F, WalkToggle.forwardInput(0F, false, true));
    assertEquals(0.3F, WalkToggle.forwardInput(0.3F, true, true));
    assertEquals(0F, WalkToggle.forwardInput(-1F, false, false));
    assertEquals(0F, WalkToggle.forwardInput(-0.3F, false, true));
  }
  @Test void headPollingDoesNotActivateOrConsumeANativeBackwardPress() {
    var state = new WalkToggle();
    state.poll(true, true, true, 0, 350); assertFalse(state.isActive());
    state.press(true, true, 10, true); assertEquals(-1F, state.input());
    state.poll(true, true, true, 400_000_010L, 350); assertEquals(-1F, state.input());
    state.poll(false, true, true, 500_000_010L, 350); assertFalse(state.isActive());
  }
  @Test void completeNativeTapsAndMenuCancellationRequireFreshPress() {
    var state = new WalkToggle();
    state.press(true, false, 0, false);
    state.poll(false, true, false, 1, 350); assertTrue(state.isActive());
    state.press(true, false, 2, false); assertFalse(state.isActive());
    state.poll(true, false, false, 3, 350);
    state.press(true, false, 4, false); assertFalse(state.isActive());
    state.poll(false, true, false, 5, 350);
    state.press(true, false, 6, true); assertEquals(-1F, state.input());
  }
  @Test void configuredChordReleaseCompletesAHoldWithoutAnotherActivation() {
    var state = new WalkToggle();
    state.press(true, true, 0, true);
    state.poll(false, true, true, 400_000_000L, 350); assertFalse(state.isActive());
    state.poll(true, true, true, 500_000_000L, 350); assertFalse(state.isActive());
  }
}
