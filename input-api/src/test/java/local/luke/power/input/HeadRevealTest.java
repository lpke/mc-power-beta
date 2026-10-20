package local.luke.power.input;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HeadRevealTest {
  @Test void onlyInitialOverlapHidesAndEveryVisitStartsFresh() {
    var reveal = new HeadReveal(); assertFalse(reveal.hidden());
    reveal.begin(); reveal.update(0, .2, 0, 0, 0); assertTrue(reveal.hidden());
    reveal.update(.4, .2, 0, 0, 0); assertFalse(reveal.hidden());
    reveal.update(0, .2, 0, 0, 0); assertFalse(reveal.hidden());
    reveal.begin(); reveal.update(0, .2, 0, 70, 0); assertTrue(reveal.hidden());
    reveal.end(); assertFalse(reveal.hidden());
  }
  @Test void EntryFromThirdPersonNeverHidesLaterWhenFlyingInside() {
    var reveal = new HeadReveal(); reveal.begin(); reveal.update(0, .2, -4, 0, 0);
    assertFalse(reveal.hidden()); reveal.update(0, .2, 0, 0, 0); assertFalse(reveal.hidden());
  }
}
