package local.luke.power.input;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PhotoProjectionTest {
  @Test void sameAspectRetainsFramingAcrossResolutionAndGuiScale() {
    assertEquals(PhotoProjection.width(1920, 1080), PhotoProjection.width(3840, 2160));
    assertEquals(PhotoProjection.width(2560, 1080), PhotoProjection.width(5120, 2160));
    assertEquals(1080, PhotoProjection.height());
  }
  @Test void equalFractionPointerDragsPanTheSameDistance() {
    assertEquals(100 * PhotoProjection.pointerScale(1080), 200 * PhotoProjection.pointerScale(2160));
    assertEquals(1080 * 5120d / 2160, PhotoProjection.width(5120, 2160));
    assertTrue(Double.isFinite(PhotoProjection.width(0, 0)));
  }

  @Test void viewportCornersStayInsideActualRendererFootprint() {
    for (double spanX : new double[]{48, 208, 416})
      for (double spanZ : new double[]{48, 272, 416})
        for (double tilt : new double[]{10, 30, 60, 90})
          for (double yaw : new double[]{0, 22.5, 45, 90, 135, 225, 405})
            for (int[] size : new int[][]{{1920, 1080}, {3440, 1440}, {720, 1280}}) {
              double height = PhotoProjection.fittedHeight(spanX, spanZ, tilt, yaw, size[0], size[1]);
              double width = height * size[0] / size[1];
              double angle = Math.toRadians(yaw), vertical = Math.sin(Math.toRadians(tilt));
              for (int x : new int[]{-1, 1}) for (int y : new int[]{-1, 1}) {
                double projectedX = x * width / 2, projectedZ = -y * height / (2 * vertical);
                double worldX = Math.cos(angle) * projectedX - Math.sin(angle) * projectedZ;
                double worldZ = Math.sin(angle) * projectedX + Math.cos(angle) * projectedZ;
                assertTrue(Math.abs(worldX) <= spanX / 2 + 1e-8);
                assertTrue(Math.abs(worldZ) <= spanZ / 2 + 1e-8);
              }
            }
  }

  @Test void fitTracksDistanceAspectAndRotationWithoutChangingTilt() {
    double height = PhotoProjection.fittedHeight(208, 208, 30, 45, 1920, 1080);
    assertEquals(height * 2, PhotoProjection.fittedHeight(416, 416, 30, 45, 1920, 1080), 1e-8);
    assertEquals(height, PhotoProjection.fittedHeight(208, 208, 30, 45, 3840, 2160));
    assertEquals(1, PhotoProjection.zoomFactor(1.25));
    assertEquals(2, PhotoProjection.zoomFactor(2.25));
    for (double tilt : new double[]{0, 0.001, 90}) {
      double fitted = PhotoProjection.fittedHeight(208, 208, tilt, 45, 0, 0);
      assertTrue(Double.isFinite(fitted) && fitted > 0);
    }
  }
}
