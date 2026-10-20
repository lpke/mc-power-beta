package local.luke.power.ui;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DebugAxesTest {
  @Test void modernPositiveAxesFollowSmoothCameraRotation() {
    assertArrayEquals(new double[]{-10, 0}, DebugAxes.endpoint(0, 0, 0), 1e-9);
    assertArrayEquals(new double[]{0, -10}, DebugAxes.endpoint(1, 0, 0), 1e-9);
    assertArrayEquals(new double[]{0, 0}, DebugAxes.endpoint(2, 0, 0), 1e-9);
    assertArrayEquals(new double[]{-10, 0}, DebugAxes.endpoint(2, 90, 0), 1e-9);
    var a = DebugAxes.endpoint(0, 35, 23); var b = DebugAxes.endpoint(0, 35.01, 23);
    assertTrue(Math.abs(a[0] - b[0]) < .01); assertNotEquals(a[0], b[0]);
  }
}
