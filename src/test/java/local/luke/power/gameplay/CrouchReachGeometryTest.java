package local.luke.power.gameplay;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CrouchReachGeometryTest {
  @Test void higherCameraReachesThreeBlocksDownWithoutMovingTheRay() {
    for (double eye : new double[]{1.27, 1.405, 1.54, 1.62}) {
      double horizontal = .2, down = 4 + eye - .95;
      float pitch = (float) Math.toDegrees(Math.atan2(down, horizontal));
      double distance = Math.hypot(horizontal, down);
      assertTrue(distance > 4);
      assertTrue(distance < CrouchReachGeometry.range(eye, pitch));
      if (eye >= 1.54) assertTrue(distance > 4.5, "fixture must fail with the old scalar reach");
    }
  }

  @Test void compensatedEndpointLiesOnModernCrouchSphere() {
    for (double eye : new double[]{1.405, 1.54, 1.62}) for (float pitch : new float[]{10, 45, 85, 90}) {
      double reach = CrouchReachGeometry.range(eye, pitch), angle = Math.toRadians(pitch);
      double horizontal = reach * Math.cos(angle), vertical = eye - 1.27 - reach * Math.sin(angle);
      assertEquals(4.5, Math.hypot(horizontal, vertical), 1e-12);
      assertTrue(reach <= 4.85);
    }
  }

  @Test void horizontalUpwardAndLowerEyesKeepOrdinaryModernRange() {
    for (float pitch : new float[]{-90, -45, 0}) assertEquals(4.5, CrouchReachGeometry.range(1.62, pitch));
    for (float pitch : new float[]{0, 45, 90}) assertEquals(4.5, CrouchReachGeometry.range(1.27, pitch));
    assertEquals(4.5, CrouchReachGeometry.range(.4, 90));
  }

  @Test void invalidInputsAndEyeOffsetsCannotCreateUnboundedReach() {
    assertEquals(4.5, CrouchReachGeometry.range(Double.NaN, 90));
    assertEquals(4.5, CrouchReachGeometry.range(1.62, Float.NaN));
    assertEquals(4.5, CrouchReachGeometry.range(1.62, 91));
    assertEquals(4.85, CrouchReachGeometry.range(100, 90), 1e-12);
  }
}
