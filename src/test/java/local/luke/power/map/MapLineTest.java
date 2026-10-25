package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MapLineTest {
  private final MapProjection view = new MapProjection(0, 0, 1, 10, 20, 100, 80, 0);
  private final double[] output = new double[4];

  @Test void clipsOriginalSegmentsWithoutInventingFrameEdges() {
    assertFalse(MapLine.clip(view, false, -100, -100, 200, -100, output));
    assertFalse(MapLine.clip(view, true, -100, -100, 200, -100, output));
    assertTrue(MapLine.clip(view, false, -50, 60, 200, 60, output));
    assertArrayEquals(new double[]{10, 60, 110, 60}, output, 1e-9);
    assertFalse(MapLine.clip(view, false, -50, 5, 0, 5, output));
  }

  @Test void circularClippingKeepsOnlyTheOriginalChord() {
    assertTrue(MapLine.clip(view, true, -50, 80, 200, 80, output));
    double inset = 50 * Math.sqrt(.75);
    assertArrayEquals(new double[]{60 - inset, 80, 60 + inset, 80}, output, 1e-9);
    assertTrue(MapLine.clip(view, true, 60, 60, 60, 60, output));
    assertFalse(MapLine.clip(view, true, 0, 0, 0, 0, output));
  }

  @Test void rotatedBoundariesRemainOnTheirProjectedLine() {
    for (double angle : new double[]{0, .3, 1.5, 3}) {
      var rotated = new MapProjection(20, -10, 2, 10, 20, 100, 80, angle);
      var a = rotated.screen(-100, -10); var b = rotated.screen(100, -10);
      for (boolean circle : new boolean[]{false, true}) {
        assertTrue(MapLine.clip(rotated, circle, a.x(), a.y(), b.x(), b.y(), output));
        for (int i = 0; i < 4; i += 2) {
          assertTrue(rotated.contains(output[i], output[i + 1], circle, -1e-7));
          assertEquals(0, (output[i] - a.x()) * (b.y() - a.y())
              - (output[i + 1] - a.y()) * (b.x() - a.x()), 1e-7);
        }
      }
    }
  }
}
