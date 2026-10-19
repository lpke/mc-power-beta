package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class MapNavigationTest {
  @Test
  void negativeCoordinatesUseFloorDivisionAcrossEveryLevel() {
    for (int level = 0; level <= MapMath.MAX_LEVEL; level++)
      for (int coordinate :
          new int[] {-32000000, -8193, -65, -64, -1, 0, 1, 63, 64, 8192, 32000000}) {
        var key = MapMath.key(-1, 7, level, coordinate, coordinate);
        assertTrue(key.minX() <= coordinate);
        assertTrue(key.minX() + key.span() > coordinate);
      }
    assertEquals(4095, MapMath.index(-1, -1));
    assertEquals(0, MapMath.index(-64, -64));
  }

  @Test
  void zoomKeepsTheBlockBeneathTheCursorAndClampsAtBothEnds() {
    var view = new MapMath.View(-345.75, 1820.125);
    double x = view.worldX(32, 100), z = view.worldZ(150, 100);
    for (double multiplier : new double[] {1.5, 2, .25, 100000, .000001, 2}) {
      view.zoom(multiplier, -68, 50);
      assertEquals(x, view.worldX(32, 100), 1e-8);
      assertEquals(z, view.worldZ(150, 100), 1e-8);
      assertTrue(view.scale >= 1d / 128 && view.scale <= 16);
    }
  }

  @Test
  void projectionRoundtripsAcrossRotationsAndClipsTerrainWithTextureCoordinates() {
    for (double angle : new double[] {0, Math.PI / 2, Math.PI, Math.PI / 4, -1.33}) {
      var projection = new MapProjection(-12000.25, 60000.75, .75, 10, 20, 128, 128, angle);
      var screen = projection.screen(-12020, 60040);
      var point = projection.world(screen.x(), screen.y());
      assertEquals(-12020, point.x(), 1e-8);
      assertEquals(60040, point.y(), 1e-8);
      var large =
          List.of(
              new MapProjection.Point(-100, -100, 0, 0),
              new MapProjection.Point(300, -100, 1, 0),
              new MapProjection.Point(300, 300, 1, 1),
              new MapProjection.Point(-100, 300, 0, 1));
      for (boolean circle : new boolean[] {false, true})
        for (var p : projection.clip(large, circle)) {
          assertTrue(p.x() >= 9.99 && p.x() <= 138.01 && p.y() >= 19.99 && p.y() <= 148.01);
          assertTrue(p.u() >= 0 && p.u() <= 1 && p.v() >= 0 && p.v() <= 1);
        }
    }
  }

  @Test
  void distantWaypointBearingsStayInsideBothMinimapShapes() {
    var view = new MapProjection(0, 0, 1, 10, 20, 108, 108, 0);
    for (boolean circle : new boolean[] {false, true})
      for (int angle = 0; angle < 360; angle++) {
        double radians = Math.toRadians(angle);
        var distant =
            new MapProjection.Point(
                64 + 1000 * Math.cos(radians), 74 + 1000 * Math.sin(radians), 0, 0);
        var edge = view.edge(distant, circle, 7);
        assertTrue(view.contains(edge.x(), edge.y(), circle, 6.99));
        assertEquals(
            Math.atan2(distant.y() - 74, distant.x() - 64),
            Math.atan2(edge.y() - 74, edge.x() - 64),
            1e-10);
      }
  }

  @Test
  void completeCoverageStaysBoundedAtWideViewsAndWorldBorders() {
    for (int width : new int[] {320, 1920, 3840, 7680, 16384})
      for (double scale : new double[] {1d / 128, .125, 1, 16})
        for (double angle : new double[] {0, .7, Math.PI / 2}) {
          var view =
              new MapProjection(MapMath.BORDER, 0, scale, 0, 0, width, width * 9d / 16, angle);
          var bounds = MapCoverage.select(view, false, 64);
          assertTrue(bounds.count() <= 64, "tile count " + bounds);
          int span = MapMath.SIZE << bounds.level();
          assertTrue((long) bounds.minX() * span <= MapMath.BORDER);
          assertTrue(((long) bounds.maxX() + 1) * span > MapMath.BORDER);
          assertDoesNotThrow(
              () -> new MapMath.Key(0, -1, bounds.level(), bounds.minX(), bounds.minZ()));
          assertDoesNotThrow(
              () -> new MapMath.Key(0, -1, bounds.level(), bounds.maxX(), bounds.maxZ()));
        }
  }

  @Test
  void settingsRejectNonfiniteAndOutOfRangeValues() {
    var s = new MapSettings();
    s.validate();
    s.zoom = Double.NaN;
    assertThrows(IllegalArgumentException.class, s::validate);
    assertThrows(
        IllegalArgumentException.class, () -> new MapMath.View(0, 0).zoom(Double.NaN, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> new MapMath.Key(0, -2, 0, 0, 0));
  }
}
