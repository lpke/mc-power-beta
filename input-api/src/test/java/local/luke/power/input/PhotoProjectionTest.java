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
}
