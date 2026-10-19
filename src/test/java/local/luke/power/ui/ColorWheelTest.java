package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ColorWheelTest {
  @Test void rgbRoundTripsAcrossChannelsAndHueBoundaries() {
    for (int red = 0; red <= 255; red += 17)
      for (int green = 0; green <= 255; green += 17)
        for (int blue = 0; blue <= 255; blue += 17) {
          int rgb = red << 16 | green << 8 | blue;
          assertEquals(rgb, ColorWheel.fromRgb(rgb, null).rgb());
        }
    assertEquals(0xff0000, new ColorWheel.Hsv(1, 1, 1).rgb());
    assertEquals(0xff00ff, new ColorWheel.Hsv(-1d / 6, 1, 1).rgb());
  }

  @Test void blackAndGreyRetainHueWhileBrightnessCanRestoreBlack() {
    var original = ColorWheel.fromRgb(0x762bc4, null);
    var black = ColorWheel.fromRgb(0, original);
    assertEquals(original.hue(), black.hue());
    assertEquals(original.saturation(), black.saturation());
    assertEquals(0x762bc4, black.brightness(original.brightness()).rgb());
    var grey = ColorWheel.fromRgb(0x777777, original);
    assertEquals(original.hue(), grey.hue());
    assertEquals(0, grey.saturation());
    assertEquals(0x777777, grey.rgb());
  }

  @Test void draggingClampsToTheRimAndCenterKeepsHueAndBrightness() {
    var bounds = new ColorWheel.Bounds(60, 70, 40);
    var original = new ColorWheel.Hsv(.7, .5, .4);
    assertTrue(bounds.contains(100, 70));
    assertFalse(bounds.contains(101, 70));
    var rim = bounds.select(original, 160, 70);
    assertEquals(1, rim.saturation());
    assertEquals(.4, rim.brightness());
    assertEquals(0, rim.hue());
    var center = bounds.select(original, 60, 70);
    assertEquals(.7, center.hue());
    assertEquals(0, center.saturation());
    assertEquals(.4, center.brightness());
  }

  @Test void selectedMarkerCoordinatesUseTheSameWheelGeometry() {
    var bounds = new ColorWheel.Bounds(42, 91, 23);
    for (double hue : new double[]{0, .1, .25, .5, .75, .99}) {
      for (double saturation : new double[]{.1, .5, 1}) {
        var color = new ColorWheel.Hsv(hue, saturation, .8);
        var point = bounds.marker(color);
        var selected = bounds.select(color, point.x(), point.y());
        assertEquals(hue, selected.hue(), .0000001);
        assertEquals(saturation, selected.saturation(), .0000001);
        assertEquals(color.rgb(), selected.rgb());
      }
    }
  }
}
