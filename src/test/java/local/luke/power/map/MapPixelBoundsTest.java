package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MapPixelBoundsTest {
  @Test void fullWindowAndSidebarUseTheirOwnPixels() {
    assertEquals(new MapPixelBounds(0, 0, 5120, 2160),
        pixels(0, 0, 1280, 540, 1280, 540, 5120, 2160));
    assertEquals(new MapPixelBounds(1720, 0, 3400, 2160),
        pixels(430, 0, 850, 540, 1280, 540, 5120, 2160));
    assertEquals(new MapPixelBounds(0, 0, 794, 716),
        pixels(0, 0, 265, 239, 265, 239, 794, 716));
  }

  @Test void oddDimensionsScaleAxesIndependentlyAndExcludeOutsidePixels() {
    var bounds = pixels(48, 0, 80, 128, 128, 128, 257, 259);
    assertEquals(new MapPixelBounds(97, 0, 160, 259), bounds);
    assertEquals(257, bounds.x() + bounds.width());
  }

  @Test void topLeftGuiOriginBecomesBottomLeftFramebufferOrigin() {
    assertEquals(new MapPixelBounds(40, 70, 120, 200),
        pixels(10, 6, 30, 40, 100, 60, 400, 300));
  }

  private static MapPixelBounds pixels(double x, double y, double w, double h,
      int guiWidth, int guiHeight, int pixelWidth, int pixelHeight) {
    return MapPixelBounds.of(new MapProjection(0, 0, 1, x, y, w, h, 0),
        guiWidth, guiHeight, pixelWidth, pixelHeight);
  }
}
