package local.luke.power.building.mining;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AutoMineToggleTest {
  @Test void pressTogglesOnceAndReleaseDoesNotAttackAgain() {
    var state = new AutoMineToggle();
    state.update(true, true); assertTrue(state.active());
    state.update(true, true); assertTrue(state.active());
    state.update(false, true); assertTrue(state.active());
    state.update(true, true); assertFalse(state.active());
    state.update(false, true); assertFalse(state.active());
  }
  @Test void cancellationRequiresReleaseBeforeAnyRestart() {
    var state = new AutoMineToggle();
    state.update(true, true);
    state.update(true, false); assertFalse(state.active());
    state.update(true, true); assertFalse(state.active());
    state.update(false, true); state.update(true, true); assertTrue(state.active());
    state.stop(); state.update(true, true); assertFalse(state.active());
    state.update(false, true); state.update(true, true); assertTrue(state.active());
  }
  @Test void modifierReleaseAndRepressCannotRepeatAHeldActionKey() {
    var state = new AutoMineToggle();
    state.update(true, true, true); assertTrue(state.active());
    state.update(true, true, false); assertTrue(state.active());
    state.update(true, true, true); assertTrue(state.active());
    state.update(false, true, false);
    state.update(true, true, true); assertFalse(state.active());
    state.update(false, true, false);
    state.update(true, true, false); state.update(true, true, true);
    assertFalse(state.active());
  }
  @Test void headPollingCannotActivateOrConsumeTheQueuedNativePress() {
    var state = new AutoMineToggle();
    state.poll(true, true); assertFalse(state.active());
    state.press(true, true); assertTrue(state.active());
    state.poll(true, true); assertTrue(state.active());
    state.release(); state.press(true, true); assertFalse(state.active());
  }
  @Test void nativeFullTapRearmsAndDebugDeniedPressNeverStartsMining() {
    var state = new AutoMineToggle();
    state.press(true, false); state.release(); assertFalse(state.active());
    state.press(true, true); state.release(); assertTrue(state.active());
    state.poll(true, false); state.press(true, true); assertFalse(state.active());
    state.release(); state.press(true, true); assertTrue(state.active());
  }
}
