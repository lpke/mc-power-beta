package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class WaypointRowClicksTest {
  @Test void nativeEventTimesRecognizeFastClicksEvenWhenProcessedTogether() {
    var clicks = new WaypointRowClicks();
    assertFalse(clicks.press("home", 1_000_000_000L));
    clicks.release();
    assertTrue(clicks.press("home", 1_250_000_000L));
    assertFalse(clicks.press("home", 1_300_000_000L), "an edit consumes both clicks");
  }

  @Test void elapsedNativeTimeAndRowIdentityPreventFalseDoubleClicks() {
    var clicks = new WaypointRowClicks();
    assertFalse(clicks.press("home", 1_000_000_000L));
    clicks.release();
    assertFalse(clicks.press("home", 1_250_000_001L));
    clicks.release();
    assertFalse(clicks.press("mine", 1_300_000_000L));
    clicks.release();
    assertFalse(clicks.press("mine", 1_200_000_000L), "out-of-order timestamps cannot edit");
  }

  @Test void releaseAndUninterruptedRowOwnershipAreRequired() {
    var clicks = new WaypointRowClicks();
    assertFalse(clicks.press("home", 1_000_000_000L));
    assertFalse(clicks.press("home", 1_100_000_000L));
    clicks.release();
    clicks.clear();
    assertFalse(clicks.press("home", 1_200_000_000L));
    clicks.release();
    assertFalse(clicks.press("home", 1_200_000_000L), "duplicate timestamps cannot edit");
  }
}
