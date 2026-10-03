package local.luke.autowalk;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class WalkToggleTest {
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
