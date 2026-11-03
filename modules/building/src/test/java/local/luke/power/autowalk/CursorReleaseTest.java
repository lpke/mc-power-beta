package local.luke.power.autowalk;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CursorReleaseTest {
  @Test void gameplayMotionStopsAtTheReleaseMarkerAndNewMotionStartsImmediately() {
    var release = new CursorRelease();
    release.grabbed(true);
    assertTrue(release.restore(640, 360)); release.warpIssued();
    release.grabbed(false);
    assertTrue(release.pending(), "older gameplay motion must not replace the GUI center");
    assertEquals(640, release.x()); assertEquals(360, release.y());
    release.warpHandled();
    assertFalse(release.pending(), "the very next deliberate motion must be accepted");
  }

  @Test void anOlderEdgeWarpAtTheSameCenterCannotReleaseTheBoundary() {
    var release = new CursorRelease(); release.grabbed(true);
    release.warpIssued(); release.warpIssued();
    assertTrue(release.restore(640, 360)); release.warpIssued(); release.grabbed(false);
    release.warpHandled(); assertTrue(release.pending());
    release.warpHandled(); assertTrue(release.pending());
    release.warpHandled(); assertFalse(release.pending());
  }

  @Test void menuNavigationNeverRequestsAnotherWarp() {
    var release = new CursorRelease();
    assertFalse(release.restore(640, 360));
    assertFalse(release.pending());
    release.grabbed(true); assertTrue(release.restore(640, 360));
    release.warpIssued(); release.grabbed(false); release.warpHandled();
    assertFalse(release.restore(20, 30));
    assertFalse(release.pending());
    assertEquals(640, release.x()); assertEquals(360, release.y());
  }

  @Test void inventoryLookingRestoresItsOriginAcrossConsecutiveFastCaptures() {
    var release = new CursorRelease();
    release.grabbed(true); assertTrue(release.restore(40, 90)); release.warpIssued();
    release.grabbed(false);
    release.grabbed(true); assertTrue(release.restore(80, 120)); release.warpIssued();
    release.grabbed(false);
    assertEquals(80, release.x()); assertEquals(120, release.y());
    release.warpHandled(); assertTrue(release.pending());
    release.warpHandled(); assertFalse(release.pending());
    release.grabbed(true); release.warpIssued();
    assertFalse(release.pending(), "ordinary captured edge warps must keep their native motion");
  }
}
