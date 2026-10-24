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

  @Test void retainsOriginalZoomScaleAndFullZoomOutRange() {
    for (double zoom : new double[]{0.01, 1.25, 2.25, 12})
      assertEquals(Math.pow(2, zoom), PhotoProjection.zoomFactor(zoom));
    assertEquals(1072.539895, PhotoProjection.height() / PhotoProjection.zoomFactor(0.01), 1e-5);
    assertEquals(454.084064, PhotoProjection.height() / PhotoProjection.zoomFactor(1.25), 1e-5);
    assertEquals(2, PhotoProjection.zoomFactor(2.25) / PhotoProjection.zoomFactor(1.25), 1e-10);
  }
}
