package local.luke.power.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SlotDoubleClickTest {
  @Test void consecutiveLeftPressesUseNativeTimeAndOneSlot() {
    var clicks = new SlotDoubleClick();
    assertFalse(clicks.press(9, 0, 1_000_000_000L));
    assertTrue(clicks.press(9, 0, 1_200_000_000L));
    assertFalse(clicks.press(9, 0, 1_210_000_000L));
  }
  @Test void unrelatedSlotsButtonsAndLongIntervalsDoNotOpenThePicker() {
    var clicks = new SlotDoubleClick();
    assertFalse(clicks.press(9, 0, 1_000_000_000L));
    assertFalse(clicks.press(10, 0, 1_100_000_000L));
    assertFalse(clicks.press(10, 1, 1_150_000_000L));
    assertFalse(clicks.press(10, 0, 1_200_000_000L));
    assertFalse(clicks.press(10, 0, 1_500_000_001L));
  }
  @Test void PaintingFocusLossAndNavigationCancelThePair() {
    var clicks = new SlotDoubleClick();
    clicks.press(9, 0, 100); clicks.cancel();
    assertFalse(clicks.press(9, 0, 200));
    assertFalse(clicks.press(9, 0, 200));
    assertFalse(clicks.press(9, 0, 150));
  }
}
