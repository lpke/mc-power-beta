package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class MapPolygonTest {
  @Test void reusedStoragePreservesEveryClippedCoordinateAndTextureCoordinate() {
    var random = new Random(430);
    var actual = new MapPolygon();
    for (int trial = 0; trial < 12000; trial++) {
      double worldX = random.nextDouble(-30_000_000, 30_000_000);
      double worldZ = random.nextDouble(-30_000_000, 30_000_000);
      var view = new MapProjection(worldX, worldZ, Math.pow(2, random.nextInt(-6, 5)),
          random.nextDouble(-200, 600), random.nextDouble(-200, 600),
          random.nextDouble(10, 1500), random.nextDouble(10, 1500), random.nextDouble(-Math.PI, Math.PI));
      double x0 = worldX + random.nextDouble(-1000, 1000), z0 = worldZ + random.nextDouble(-1000, 1000);
      double x1 = x0 + random.nextDouble(1, 2048), z1 = z0 + random.nextDouble(1, 2048);
      double u0 = random.nextDouble(), v0 = random.nextDouble(), u1 = random.nextDouble(), v1 = random.nextDouble();
      boolean circle = random.nextBoolean();
      var expected = view.clip(List.of(point(view, x0, z0, u0, v0), point(view, x1, z0, u1, v0),
          point(view, x1, z1, u1, v1), point(view, x0, z1, u0, v1)), circle);
      actual.project(view, x0, z0, x1, z1, u0, v0, u1, v1).clip(view, circle);
      same(expected, actual);
      actual.rectangle(view.left, view.top, view.left + view.width, view.top + view.height).clip(view, circle);
      same(view.clip(List.of(new MapProjection.Point(view.left, view.top, 0, 0),
          new MapProjection.Point(view.left + view.width, view.top, 1, 0),
          new MapProjection.Point(view.left + view.width, view.top + view.height, 1, 1),
          new MapProjection.Point(view.left, view.top + view.height, 0, 1)), circle), actual);
    }
  }

  private static MapProjection.Point point(MapProjection view, double x, double z, double u, double v) {
    var p = view.screen(x, z);
    return new MapProjection.Point(p.x(), p.y(), u, v);
  }

  private static void same(List<MapProjection.Point> expected, MapPolygon actual) {
    assertEquals(expected.size(), actual.size());
    for (int i = 0; i < expected.size(); i++) {
      var p = expected.get(i);
      assertEquals(Double.doubleToLongBits(p.x()), Double.doubleToLongBits(actual.value(i, 0)));
      assertEquals(Double.doubleToLongBits(p.y()), Double.doubleToLongBits(actual.value(i, 1)));
      assertEquals(Double.doubleToLongBits(p.u()), Double.doubleToLongBits(actual.value(i, 2)));
      assertEquals(Double.doubleToLongBits(p.v()), Double.doubleToLongBits(actual.value(i, 3)));
    }
  }
}
