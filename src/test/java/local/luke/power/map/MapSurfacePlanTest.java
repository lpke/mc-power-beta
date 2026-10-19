package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.FloatBuffer;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class MapSurfacePlanTest {
  @Test void nativeUltrawideKeepsFineDetailRegardlessOfCpuCache() {
    var view = new MapProjection(-300, 500, .25, 0, 0, 1280, 540, 0);
    var plan = MapSurfacePlan.create(view, 4, 16384, true);
    assertNotNull(plan);
    assertEquals(5248, plan.width());
    assertEquals(2288, plan.height());
    assertEquals(0, plan.level());
    assertTrue((long) plan.width() * plan.height() * 8 < MapSurfacePlan.MAX_PAIR_BYTES);
    assertTrue(MapCoverage.bounds(view, false, plan.level()).count() > 1024);
    assertNull(MapSurfacePlan.create(view, 4, 4096, true));
    assertNull(MapSurfacePlan.create(view, 8, 16384, true));
  }

  @Test void detailBreaksOnlyAtPhysicalPixelThresholdAndManualAlwaysRequestsLeaves() {
    for (int gui = 1; gui <= 4; gui++) {
      for (double physical : new double[] {.125, .249999, .25, .499999, .5, .9999, 1, 4}) {
        var view = new MapProjection(0, 0, physical / gui, 0, 0, 1920d / gui, 1080d / gui, 0);
        assertEquals(MapMath.level(physical), MapSurfacePlan.create(view, gui, 8192, true).level());
        assertEquals(0, MapSurfacePlan.create(view, gui, 8192, false).level());
      }
    }
  }

  @Test void centerOutSweepVisitsEachCellOnceWithoutRetainingATileList() {
    var visited = new HashSet<MapTileSweep.Cell>();
    for (int i = 0; i < 10201; i++) assertTrue(visited.add(MapTileSweep.cell(i)), "duplicate " + i);
    assertEquals(new MapTileSweep.Cell(0, 0), MapTileSweep.cell(0));
    assertTrue(visited.contains(new MapTileSweep.Cell(-50, -50)));
    assertTrue(visited.contains(new MapTileSweep.Cell(50, 50)));
  }

  private static FloatBuffer identity() {
    var matrix = FloatBuffer.allocate(16);
    for (int i = 0; i < 4; i++) matrix.put(i * 5, 1);
    return matrix;
  }

  @Test void projectionRetainsSubpixelMotionAndRejectsBehindCamera() {
    var model = identity(); var projection = identity();
    var a = WaypointProjection.project(.001, .002, 0, model, projection);
    assertEquals(.5005, a.x(), 1e-12);
    assertEquals(.501, a.y(), 1e-12);
    model.put(12, .25f);
    assertEquals(.5, WaypointProjection.project(-.25, 0, 0, model, projection).x(), 1e-12);
    projection.put(15, -1);
    assertNull(WaypointProjection.project(0, 0, 0, model, projection));
    projection.put(15, 1);
    assertNull(WaypointProjection.project(0, 0, 2, model, projection));
  }
}
