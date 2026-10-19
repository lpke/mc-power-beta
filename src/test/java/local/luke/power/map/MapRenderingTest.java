package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MapRenderingTest {
  @Test
  void bufferPaddingDoesNotRequestTilesAndOversizedMapsUseFallback() {
    var layout = MapBufferLayout.create(108, 4, 16384);
    assertNotNull(layout);
    assertEquals(1024, layout.side());
    assertTrue(layout.coverage() < 700);
    var view = new MapProjection(0, 0, 1, 0, 0, layout.coverage(), layout.coverage(), 0);
    assertEquals(0, MapCoverage.select(view, false, 256).level());
    assertNull(MapBufferLayout.create(480, 4, 16384));
    assertNull(MapBufferLayout.create(240, 4, 1024));
    assertNull(MapBufferLayout.create(108, 4, 128));
  }

  @Test
  void terrainCadenceUsesWholeGameTicksAtEveryRate() {
    for (int rate = 1; rate <= 20; rate++) {
      var cadence = new MapCadence();
      assertTrue(cadence.tick(rate));
      int updates = 0, gap = 0;
      for (int tick = 0; tick < 400; tick++) {
        gap++;
        if (cadence.tick(rate)) {
          updates++;
          assertTrue(gap <= Math.ceil(20d / rate));
          gap = 0;
        }
      }
      assertEquals(rate * 20, updates);
    }
  }

  @Test
  void frameMotionRemainsSmoothAcrossTickAndAngleBoundaries() {
    assertEquals(10.25, MapMotion.position(10, 11, .25f));
    assertEquals(10.75, MapMotion.position(10, 11, .75f));
    assertEquals(1000, MapMotion.position(10, 1000, .25f));
    assertEquals(1000, MapMotion.position(Double.NaN, 1000, .25f));
    assertEquals(180, MapMotion.angle(179, -179, .5f));
    assertEquals(-180, MapMotion.angle(-179, 179, .5f));
  }

  @Test
  void zoomUsesPhysicalResolutionAndRetainsFineTerrain() {
    var view = new MapProjection(0, 0, .4, 0, 0, 320, 180, 0);
    assertEquals(1, MapCoverage.select(view, false, 256, 1).level());
    assertEquals(0, MapCoverage.select(view, false, 256, 4).level());
    assertTrue(MapCoverage.select(view, false, 64, 4).count() <= 64);
  }

  @Test
  void easedZoomKeepsCursorWorldPositionDuringRepeatedScrolls() {
    var view = new MapMath.View(-300, 500);
    var zoom = new MapZoom();
    double x = view.x + 42 / view.scale, z = view.z - 21 / view.scale;
    zoom.wheel(view, 1, 42, -21, 1_000_000_000L);
    zoom.update(view, 1_050_000_000L);
    assertTrue(view.scale > 1 && view.scale < 1.2);
    zoom.wheel(view, 1, 42, -21, 1_050_000_000L);
    for (long now = 1_050_000_000L; now <= 1_150_000_000L; now += 10_000_000L) {
      zoom.update(view, now);
      assertEquals(x, view.x + 42 / view.scale, 1e-9);
      assertEquals(z, view.z - 21 / view.scale, 1e-9);
    }
    assertEquals(1.44, view.scale, 1e-12);
    zoom.wheel(view, -1, 42, -21, 1_150_000_000L);
    zoom.cancel();
    zoom.update(view, 2_000_000_000L);
    assertEquals(1.44, view.scale, 1e-12);
  }

  @Test
  void reliefHasCrispDirectionalEdgesAndCoolAmbientShadows() {
    int flat = MapRelief.colour(0x808080, 64, 64, 64, 0);
    int lit = MapRelief.colour(0x808080, 65, 64, 64, 0);
    int shadow = MapRelief.colour(0x808080, 64, 65, 65, 0);
    assertEquals(0x89_8d_95, flat);
    assertEquals(0xa2_a6_ae, lit);
    assertEquals(0x4d_51_59, shadow);
    assertNotEquals(lit, MapRelief.colour(0x808080, 65, 64, 65, 0));
    int nether = MapRelief.colour(0x808080, 64, 65, 65, -1);
    assertEquals(0x59_40_40, nether);
  }

  @Test
  void smallerLevelsAverageAlreadyShadedPixelsAndKeepRawColourMode() {
    var tile = new MapTile(new MapMath.Key(0, -1, 0, 0, 0));
    for (int z = 0; z < 4; z++)
      for (int x = 0; x < 4; x++) tile.put(z * 64 + x, 0x808080, 0x606060, 64 + z % 2, 15, 1, 0);
    tile.refreshRelief(null);
    var parent = new MapTile(tile.key.parent());
    assertTrue(parent.merge(tile));
    int expected = 0;
    for (int shift : new int[] {0, 8, 16}) {
      int sum = 0;
      for (int i : new int[] {0, 1, 64, 65}) sum += tile.reliefAccurate[i] >> shift & 255;
      expected |= (sum / 4) << shift;
    }
    assertEquals(expected, parent.reliefAccurate[0]);
    var settings = new MapSettings();
    settings.heightShading = false;
    assertEquals(0xff000000 | expected, parent.colour(0, settings));
    settings.slopeShading = false;
    assertEquals(0xff808080, parent.colour(0, settings));
    assertFalse(parent.merge(tile));
  }

  @Test
  void cachedNeighbourHeightRepairsSeamsWithoutSamplingWorld() {
    var tile = new MapTile(new MapMath.Key(0, -1, 0, 0, 0));
    tile.put(0, 0x808080, 0x808080, 65, 15, 1, 0);
    tile.refreshRelief((x, z) -> 64);
    assertEquals(MapRelief.colour(0x808080, 65, 64, 64, 0), tile.reliefAccurate[0]);
    tile.invalidateRelief(0);
    tile.refreshRelief((x, z) -> 66);
    assertEquals(MapRelief.colour(0x808080, 65, 66, 66, 0), tile.reliefAccurate[0]);
    assertFalse(
        tile.refreshRelief(
            (x, z) -> {
              throw new AssertionError("clean tile resampled");
            }));
  }
}
