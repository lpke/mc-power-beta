package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MapSamplingFocusTest {
  @Test void theFullVisibleViewportOutsideTheCenterThreeByThreeOutranksOffscreenUnsurveyedChunks() {
    var view = new MapProjection(0, 0, 1, 0, 0, 512, 256, 0);
    var focus = MapSamplingFocus.of(view, 0, 0, 12);
    assertTrue(focus.contains(10, 5));
    assertFalse(focus.contains(11, 10));
    assertTrue(focus.tier(10, 5, 0, 0, false) < focus.tier(11, 10, 0, 0, false));
    assertEquals(-12, focus.minX()); assertEquals(12, focus.maxX());
    assertEquals(-8, focus.minZ()); assertEquals(8, focus.maxZ());
    for (int x = -16; x <= 16; x++) for (int z = -16; z <= 16; z++)
      if (focus.contains(x, z)) assertTrue(Math.abs(x) <= 12 && Math.abs(z) <= 12);
  }

  @Test void aPannedViewportWinsOverPlayerNeighborsAndMeasuresVisibleDistanceFromItsOwnCenter() {
    var focus = MapSamplingFocus.of(new MapProjection(104, 104, 1, 0, 0, 128, 128, 0), 0, 0, 12);
    assertTrue(focus.contains(7, 6)); assertFalse(focus.contains(0, 0));
    assertTrue(focus.tier(7, 6, 0, 0, false) < focus.tier(0, 0, 0, 0, false));
    assertTrue(focus.tier(0, 0, 0, 0, false) < focus.tier(-8, -8, 0, 0, false));
    assertEquals(1, focus.distance(7, 6, 0, 0));
    assertEquals(2, focus.distance(1, 1, 0, 0));
    assertTrue(focus.distance(7, 6, 0, 0) < focus.distance(2, 2, 0, 0));
  }

  @Test void negativeFractionalEdgesUseFloorChunkCoordinatesAndAViewOutsideLoadedRadiusStaysEmpty() {
    var focus = MapSamplingFocus.of(new MapProjection(-.25, -16.25, 1, 0, 0, 32, 16, 0), 0, 0, 8);
    assertEquals(-2, focus.minX()); assertEquals(0, focus.maxX());
    assertEquals(-2, focus.minZ()); assertEquals(-1, focus.maxZ());
    assertEquals(-1, focus.centerX()); assertEquals(-2, focus.centerZ());
    var distant = MapSamplingFocus.of(new MapProjection(100000, 100000, 1, 0, 0, 128, 128, 0), 0, 0, 4);
    for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) assertFalse(distant.contains(x, z));
    assertTrue(distant.tier(0, 0, 0, 0, false) < distant.tier(4, 4, 0, 0, false));
  }

  @Test void gameplayWithoutAMapUsesOnlyThePlayerNeighborhoodAndWorldBorderDistancesDoNotOverflow() {
    var focus = MapSamplingFocus.of(null, -2, 3, 12);
    assertTrue(focus.contains(-3, 2)); assertTrue(focus.contains(-1, 4));
    assertFalse(focus.contains(0, 3));
    assertTrue(focus.tier(-3, 2, -2, 3, false) < focus.tier(0, 3, -2, 3, false));
    assertTrue(focus.tier(-2, 3, -2, 3, true) < focus.tier(0, 3, -2, 3, false),
        "due nearby terrain must refresh before the peripheral survey finishes");
    var border = MapSamplingFocus.of(null, 2000000, -2000000, 12);
    assertEquals(32000000000000L, border.distance(-2000000, 2000000, 2000000, -2000000));
  }
}
