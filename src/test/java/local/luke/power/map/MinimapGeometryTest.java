package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class MinimapGeometryTest {
  @Test void insetsMoveBothTheMapAndItsHitBoundsInEveryCorner() {
    var settings = new MapSettings();
    for (int corner = 0; corner < 4; corner++) {
      settings.corner = corner; settings.offsetX = settings.offsetY = 20;
      var before = frame(settings, 640, 360);
      settings.offsetX += 15 * MinimapGeometry.offsetDirection(corner, true);
      settings.offsetY += 12 * MinimapGeometry.offsetDirection(corner, false);
      var after = frame(settings, 640, 360);
      assertEquals(15, after.layout().x() - before.layout().x());
      assertEquals(12, after.layout().y() - before.layout().y());
      assertEquals(15, after.bounds().x() - before.bounds().x());
      assertEquals(12, after.bounds().y() - before.bounds().y());
    }
  }
  @Test void scaledInformationAndTheWholeSolidBorderStayInsideNarrowAndWideScreens() {
    var settings = new MapSettings(); settings.clock = true; settings.size = 240;
    for (int[] screen : List.of(new int[]{128, 128}, new int[]{320, 180}, new int[]{1280, 540}))
      for (int size : new int[]{50, 100, 200}) for (int corner = 0; corner < 4; corner++) {
        settings.belowTextSize = size; settings.corner = corner;
        var frame = frame(settings, screen[0], screen[1]); var box = frame.bounds();
        assertTrue(box.x() >= 0 && box.y() >= 0);
        assertTrue(box.x() + box.width() <= screen[0]);
        assertTrue(box.y() + box.height() <= screen[1]);
        assertTrue(box.x() <= frame.layout().x() - MinimapGeometry.BORDER);
        for (var label : frame.labels()) {
          assertTrue(label.x() >= box.x());
          assertTrue(label.x() + label.text().length() * 6 * label.scale() <= box.x() + box.width());
          assertTrue(label.y() + 9 * label.scale() <= box.y() + box.height());
        }
      }
  }
  @Test void headingMovesWithinOnePixelAndKeepsSquareDirectionsOnTheSameInsetEdge() {
    var layout = new MinimapGeometry.Layout(20, 10, 108);
    var first = MinimapGeometry.direction(layout, 0, .2, false, 6, 1);
    var next = MinimapGeometry.direction(layout, 0, .2001, false, 6, 1);
    assertTrue(next.x() > first.x());
    assertTrue(next.x() - first.x() < 1);
    assertEquals(first.y(), next.y(), 1e-9);
    assertNotEquals(Math.rint(next.x()), next.x());
    var north = MinimapGeometry.direction(layout, 0, 0, true, 6, 2);
    var east = MinimapGeometry.direction(layout, 1, 0, true, 6, 2);
    assertEquals(42, east.x() - north.x(), 1e-9);
    assertEquals(42, east.y() - north.y(), 1e-9);
  }
  private static MinimapGeometry.Frame frame(MapSettings settings, int width, int height) {
    return MinimapGeometry.frame(settings, width, height, false,
        settings.clock ? List.of("32000000, 127, -32000000", "Forest", "12:00")
            : List.of("128, 64, -256", "Forest"), text -> text.length() * 6);
  }
}
