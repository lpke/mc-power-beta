package local.luke.power.autowalk;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LineLockTest {
  @Test void forwardAndBackwardRetainTheSameLaneWithoutChangingTravelMotion() {
    for (float yaw : new float[]{0, 180}) {
      var lock = new LineLock(); lock.start(3.5, -2.5, yaw);
      assertEquals(.2, lock.x(3.5, .3, 5), .00001);
      assertEquals(-.2, lock.x(3.5, .3, -5), .00001);
      assertEquals(5, lock.z(-2.5, .3, 5));
      assertEquals(-5, lock.z(-2.5, .3, -5));
    }
  }
  @Test void eastWestAndDiagonalTravelUseTheOriginalBlockAxis() {
    var lock = new LineLock(); lock.start(-.5, 8.5, 90);
    assertEquals(9, lock.x(-.5, .3, 9));
    assertEquals(.2, lock.z(8.5, .3, 9), .00001);
    lock.start(-.5, 8.5, 30);
    assertEquals(.2, lock.x(-.5, .3, 9), .00001);
    assertEquals(9, lock.z(8.5, .3, 9));
  }
  @Test void boundaryOverlapNeverTeleportsAndStoppingRestoresMovement() {
    var lock = new LineLock(); lock.start(.05, .5, 0);
    assertEquals(0, lock.x(.05, .3, -.1));
    assertEquals(.1, lock.x(.05, .3, .1), .00001);
    lock.stop(); assertEquals(-9, lock.x(.05, .3, -9));
  }
}
