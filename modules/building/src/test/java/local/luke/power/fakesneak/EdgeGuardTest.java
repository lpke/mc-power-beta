package local.luke.power.fakesneak;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EdgeGuardTest {
  @Test
  void keepsFullMotionWhenSupported() {
    for (double x : new double[] {-1, -.1, 0, .1, 1})
      for (double z : new double[] {-1, -.1, 0, .1, 1})
        assertEquals(new EdgeGuard.Motion(x, z), EdgeGuard.clip(x, z, (a, b) -> true));
  }

  @Test
  void closesDiagonalCornerHole() {
    var m = EdgeGuard.clip(1, 1, (x, z) -> Math.abs(x) < .8 || Math.abs(z) < .8);
    assertTrue(m.x() < .8 || m.z() < .8);
    assertTrue(m.x() > 0 && m.z() > 0);
  }

  @Test
  void handlesNegativeEdgesAndNoSupport() {
    var m = EdgeGuard.clip(-1, -1, (x, z) -> x > -.3 && z > -.3);
    assertTrue(m.x() > -.3 && m.z() > -.3);
    assertEquals(new EdgeGuard.Motion(0, 0), EdgeGuard.clip(.31, -.31, (x, z) -> false));
  }

  @Test
  void boundedForInvalidAndHugeMotion() {
    for (double x :
        new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 1e20, -1e20})
      assertEquals(
          new EdgeGuard.Motion(0, 0),
          EdgeGuard.clip(
              x,
              1,
              (a, b) -> {
                fail("Must not query world");
                return true;
              }));
  }

  @Test
  void randomizedEdgesNeverIncreaseSpeedOrLeaveSupport() {
    var random = new java.util.Random(173);
    for (int i = 0; i < 10000; i++) {
      double x = random.nextDouble() * 2 - 1,
          z = random.nextDouble() * 2 - 1,
          limit = .1 + random.nextDouble() * .7;
      var m = EdgeGuard.clip(x, z, (a, b) -> Math.hypot(a, b) < limit);
      assertTrue(Math.abs(m.x()) <= Math.abs(x) && Math.abs(m.z()) <= Math.abs(z));
      assertTrue(Math.hypot(m.x(), m.z()) < limit);
    }
  }
}
