package local.luke.power.transport;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.Test;

class BoatMotionTest {
  @Test
  void turnsFasterAtRestAndDoesNotNeedThrustToTurn() {
    BoatMotion motion = new BoatMotion();
    motion.step(0, 0, 0, 0, 1, true);
    assertEquals(5, motion.yaw);
    assertEquals(0, Math.hypot(motion.x, motion.z));
    motion.step(0.8, 0, 0, 0, 1, true);
    assertEquals(3, motion.yaw);
  }

  @Test
  void reversingBrakesMoreStronglyThanForwardAccelerates() {
    BoatMotion forward = new BoatMotion();
    BoatMotion reverse = new BoatMotion();
    forward.step(0.4, 0, 0, 1, 0, true);
    reverse.step(0.4, 0, 0, -1, 0, true);
    assertEquals(0.0065, forward.x - 0.4, 1e-6);
    assertEquals(0.013, 0.4 - reverse.x, 1e-6);
  }

  @Test
  void capsTotalSpeedInEveryDirectionWithoutDiagonalBonus() {
    BoatMotion motion = new BoatMotion();
    for (int yaw = -360; yaw <= 360; yaw++) {
      motion.step(10, 10, yaw, 1, 1, true);
      assertEquals(0.8, Math.hypot(motion.x, motion.z), 1e-12);
    }
  }

  @Test
  void anEmptyBoatDoesNotAcquirePlayerInputOrSnapItsMotion() {
    BoatMotion motion = new BoatMotion();
    motion.step(0.2, -0.3, 80, 1, 1, false);
    assertEquals(0.2, motion.x);
    assertEquals(-0.3, motion.z);
    assertEquals(80, motion.yaw);
  }

  @Test
  void sustainedControlsStayFiniteAndBounded() {
    BoatMotion motion = new BoatMotion();
    Random random = new Random(173);
    for (int tick = 0; tick < 1_000_000; tick++) {
      motion.step(
          motion.x * 0.99,
          motion.z * 0.99,
          motion.yaw,
          random.nextInt(3) - 1,
          random.nextInt(3) - 1,
          true);
      assertTrue(
          Double.isFinite(motion.x) && Double.isFinite(motion.z) && Float.isFinite(motion.yaw));
      assertTrue(Math.hypot(motion.x, motion.z) <= 0.800000000001);
    }
  }

  @Test
  void malformedMotionCannotCreateNonFinitePositions() {
    BoatMotion motion = new BoatMotion();
    for (double value :
        new double[] {
          Double.NaN,
          Double.POSITIVE_INFINITY,
          Double.NEGATIVE_INFINITY,
          Double.MAX_VALUE,
          -Double.MAX_VALUE
        }) {
      motion.step(value, value, Float.NaN, Float.NaN, Float.POSITIVE_INFINITY, true);
      assertTrue(
          Double.isFinite(motion.x) && Double.isFinite(motion.z) && Float.isFinite(motion.yaw));
      assertTrue(Math.hypot(motion.x, motion.z) <= 0.800000000001);
    }
  }
}
