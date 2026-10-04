package local.luke.power.autowalk;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class WalkToggleTest {
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
}
