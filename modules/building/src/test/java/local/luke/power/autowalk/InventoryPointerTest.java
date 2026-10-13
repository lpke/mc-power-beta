package local.luke.power.autowalk;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InventoryPointerTest {
  @Test void completeBurstsKeepOriginAndEveryDelta() {
    InventoryPointer p = new InventoryPointer();
    p.event(90, 400, 0, 0, false);
    for (int i = 1; i <= 10; i++) {
      p.press(); p.event(90 + 24*i, 400 - 12*i, 0, 0, false); p.release();
    }
    assertEquals(90, p.x()); assertEquals(400, p.y());
    assertEquals(240, p.takeX()); assertEquals(-120, p.takeY());
    assertFalse(p.hasMotion()); assertTrue(p.restore());
  }
  @Test void movementBetweenDragsMovesCursorButNotCamera() {
    InventoryPointer p = new InventoryPointer();
    p.event(50, 60, 0, 0, false); p.press(); p.event(75, 70, 0, 0, false); p.release();
    p.event(80, 77, 0, 0, false);
    assertEquals(55, p.x()); assertEquals(67, p.y());
    p.press(); p.event(90, 80, 0, 0, false); p.release();
    assertEquals(55, p.x()); assertEquals(67, p.y());
    assertEquals(35, p.takeX()); assertEquals(13, p.takeY());
  }
  @Test void capturedReleaseAndRepressUseRelativeEventsWithoutClipping() {
    InventoryPointer p = new InventoryPointer();
    p.event(90, 400, 0, 0, false); p.press(); p.batchFinished(true);
    p.event(0, 0, -1000, 1500, true); p.release();
    p.event(0, 0, 10, -20, true); p.press(); p.event(0, 0, 300, -400, true); p.release();
    assertEquals(100, p.x()); assertEquals(380, p.y());
    assertEquals(-700, p.takeX()); assertEquals(1100, p.takeY());
    p.batchFinished(false); p.event(100, 380, 0, 0, false);
    assertEquals(100, p.x()); assertEquals(380, p.y()); assertFalse(p.hasMotion());
  }
  @Test void delayedReleaseKeepsAbsoluteBaselineAcrossPolls() {
    InventoryPointer p = new InventoryPointer();
    p.event(90, 400, 0, 0, false); p.press(); p.batchFinished(false);
    p.event(115, 412, 0, 0, false); p.release();
    assertEquals(90, p.x()); assertEquals(400, p.y());
    assertEquals(25, p.takeX()); assertEquals(12, p.takeY());
  }
  @Test void cancelDiscardsMotionAndStartsFresh() {
    InventoryPointer p = new InventoryPointer();
    p.event(90, 400, 0, 0, false); p.press(); p.event(100, 410, 0, 0, false); p.clear();
    assertFalse(p.hasMotion()); assertFalse(p.restore());
    p.event(200, 300, 0, 0, false); p.press(); p.event(202, 303, 0, 0, false); p.release();
    assertEquals(200, p.x()); assertEquals(300, p.y());
    assertEquals(2, p.takeX()); assertEquals(3, p.takeY());
  }
}
