package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

class MapLightingTest {
  @Test void nighttimeSurfaceRetainsTorchLightAndFullBrightWithoutRewritingTerrain() throws Exception {
    var settings = new MapSettings();
    var tile = new MapTile(new MapMath.Key(0, -1, 0, 0, 0));
    tile.put(0, 0x809060, 0x809060, 63, 0, 1, 0);
    byte[] original = MapCodec.encode(tile);
    int day = tile.color(0, settings, false, 0);
    int night = tile.color(0, settings, false, 11);
    assertNotEquals(day, night);
    assertEquals(day, tile.color(0, settings, true, 11));
    settings.caveLighting = false;
    assertEquals(night, tile.color(0, settings, false, 11), "the cave toggle does not disable surface night shading");
    assertArrayEquals(original, MapCodec.encode(tile));
    tile.put(0, 0x809060, 0x809060, 63, 15, 1, 0);
    assertEquals(tile.color(0, settings, false, 0), tile.color(0, settings, false, 11));
    double last = 1;
    for (int darkness = 0; darkness <= 11; darkness++) {
      double shade = MapLighting.shade(0, false, true, false, darkness);
      assertTrue(shade <= last && shade >= .35); last = shade;
    }
    assertEquals(MapLighting.shade(0, false, true, false, 11), MapLighting.shade(0, false, true, false, 100));
  }

  @Test void caveLightingDoesNotDependOnDaylightAndFullBrightHasPriority() {
    assertEquals(MapLighting.shade(3, true, true, false, 0), MapLighting.shade(3, true, true, false, 11));
    assertEquals(1, MapLighting.shade(0, true, false, false, 11));
    assertEquals(1, MapLighting.shade(0, true, true, true, 11));
  }

  @Test void allRadarColorsDimSymmetricallyAndRetainVisibilityAtTheConfiguredDistance() {
    var settings = new MapSettings();
    for (int color : new int[]{0xffffff, 0xff3333, 0xffff33, 0xaa55ff}) {
      assertEquals(color, MapLighting.radarColor(color, 0, settings));
      int far = MapLighting.radarColor(color, settings.radarDimDistance, settings);
      assertNotEquals(color, far); assertNotEquals(0, far);
      assertEquals(far, MapLighting.radarColor(color, -settings.radarDimDistance, settings));
      assertEquals(far, MapLighting.radarColor(color, 1000, settings));
      settings.radarDimming = false;
      assertEquals(color, MapLighting.radarColor(color, 1000, settings));
      settings.radarDimming = true;
    }
    settings.lightRadius = 8; settings.radarDimDistance = 1; settings.validate();
    var restored = new Gson().fromJson(new Gson().toJson(settings), MapSettings.class);
    assertEquals(8, restored.lightRadius); assertEquals(1, restored.radarDimDistance);
    restored.lightRadius = 129; assertThrows(IllegalArgumentException.class, restored::validate);
  }
}
