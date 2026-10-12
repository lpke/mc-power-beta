package local.luke.power.render;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TntMotionTest {
  @Test void deferredTicksDoNotReplayThePreviousMovement() {
    var motion = new TntMotion();
    motion.begin(1, 0, 10, 0); motion.target(1, 9, -1);
    assertEquals(.5, motion.x(.5f));
    motion.begin(2, 1, 9, -1);
    assertEquals(1, motion.x(0));
    motion.begin(3, 1, 9, -1);
    assertEquals(1, motion.x(.5f));
    motion.begin(4, 1, 9, -1); motion.target(4, 6, -4);
    assertEquals(1, motion.x(0));
    assertEquals(1.5, motion.x(.5f));
    motion.begin(5, 4, 6, -4);
    assertEquals(2, motion.x(0));
    assertEquals(8, motion.y(0));
    assertEquals(-2, motion.z(0));
  }
  @Test void interruptionsRemainContinuousAndDisablingResetsThePose() {
    var motion = new TntMotion();
    motion.begin(1, 0, 0, 0); motion.target(1, 0, 0);
    motion.begin(5, 1, 0, 0); motion.target(5, 0, 0);
    motion.begin(6, 5, 0, 0);
    double before = motion.x(0);
    motion.target(7, 0, 0);
    assertEquals(before, motion.x(0));
    assertTrue(motion.x(.5f) > before);
    motion.clear(); assertFalse(motion.initialized());
    motion.begin(99, 40, 20, 30);
    assertEquals(40, motion.x(.5f));
  }
}
