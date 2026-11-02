package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MusicDoubleClickTest {
  @Test void pairsOnlyTheSameTrackAndViewAndConsumesThePair() {
    var clicks = new MusicDoubleClick();
    assertFalse(clicks.press("library", "a", 20, 30, 1));
    assertFalse(clicks.press("playlist", "a", 20, 30, 2));
    assertFalse(clicks.press("playlist", "b", 20, 30, 3));
    assertTrue(clicks.press("playlist", "b", 21, 31, 4));
    assertFalse(clicks.press("playlist", "b", 21, 31, 5));
  }

  @Test void dragMovementTimeoutAndCancellationPreventPlayback() {
    var clicks = new MusicDoubleClick();
    assertFalse(clicks.press("library", "a", 20, 30, 1));
    clicks.release(28, 30);
    assertFalse(clicks.press("library", "a", 20, 30, 2));
    assertFalse(clicks.press("library", "a", 20, 30, 350_000_003L));
    clicks.cancel();
    assertFalse(clicks.press("library", "a", 20, 30, 350_000_004L));
    assertFalse(clicks.press("library", "a", 40, 30, 350_000_005L));
  }
}
