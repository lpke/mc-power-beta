package local.luke.power.creative;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class DebugKeyGestureTest {
  @Test void standalonePressTogglesOnceOnRelease() {
    var key = new DebugKeyGesture();
    key.press(false);
    assertTrue(key.pending());
    key.press(false); // Repeated key-down must not create another action.
    assertTrue(key.release());
    assertFalse(key.pending());
    assertFalse(key.release());
  }

  @Test void cyclingConsumesTheWholePressRegardlessOfReleaseOrder() {
    var key = new DebugKeyGesture();
    key.press(false);
    key.cycle();
    key.press(false);
    key.cycle();
    assertFalse(key.release());
    key.press(true); // F4 was held before F3.
    assertFalse(key.release());
    key.press(false);
    assertTrue(key.release());
  }

  @Test void focusMenusWorldAndBindingChangesDiscardPendingInput() {
    var key = new DebugKeyGesture();
    key.press(false);
    key.cancel();
    assertFalse(key.pending());
    assertFalse(key.release());
    key.cycle();
    key.press(false);
    assertTrue(key.release());
  }
}
