package local.luke.tweaks;

import static org.junit.jupiter.api.Assertions.*;

import local.luke.tweaks.camera.LookAngles;
import org.junit.jupiter.api.Test;

class LookAnglesTest {
  @Test
  void inputDoesNotBreakNativeInterpolation() {
    var a = new LookAngles();
    a.rotate(10, 200, -100);
    for (float delta = 0; delta <= 1; delta += .01F) {
      assertEquals(
          30 + (175 + (185 - 175) * delta), a.yaw(175) + (a.yaw(185) - a.yaw(175)) * delta, .0001);
      assertEquals(
          15 + (9 + (10 - 9) * delta), a.pitch(9) + (a.pitch(10) - a.pitch(9)) * delta, .0001);
    }
  }

  @Test
  void pitchClampsAndYawStaysBoundedDuringLongSessions() {
    var a = new LookAngles();
    for (int i = 0; i < 100000; i++) a.rotate(30, 10000, -10000);
    assertEquals(90, a.pitch(30));
    assertTrue(Math.abs(a.yaw(0)) < 360);
    a.rotate(30, 0, 10000);
    assertEquals(-90, a.pitch(30));
    a.rotate(30, Float.NaN, Float.POSITIVE_INFINITY);
    assertTrue(Float.isFinite(a.yaw(0)));
  }

  @Test
  void releaseClearsOffsetsImmediately() {
    var a = new LookAngles();
    a.rotate(0, 350 / .15F, -30 / .15F);
    a.clear();
    assertEquals(0, a.yaw(0));
    assertEquals(0, a.pitch(0));
    a.rotate(10, 100, -100);
    a.clear();
    assertEquals(25, a.yaw(25));
    assertEquals(10, a.pitch(10));
  }
}
