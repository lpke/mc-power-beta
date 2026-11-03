package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class WorldMapCavesTest {
  @Test void automaticModesAndHeightFollowTheCameraUntilChanged() {
    var caves = new WorldMapCaves();
    var settings = new MapSettings();
    assertFalse(caves.shown(false));
    assertTrue(caves.shown(true));
    assertEquals(41, caves.window(settings, 41.9).reference());
    caves.toggle(true);
    assertFalse(caves.shown(true));
    caves.toggle(false);
    assertTrue(caves.shown(false));
    caves.select(18);
    assertEquals(18, caves.window(settings, 73).reference());
    assertFalse(caves.following());
    caves.reset();
    assertEquals(73, caves.window(settings, 73).reference());
    assertEquals(92, caves.window(settings, 92).reference());
    assertTrue(caves.following());
    assertTrue(caves.shown(false));
    caves.center();
    assertFalse(caves.shown(false));
  }

  @Test void exactHeightUsesSeparateCacheContextsAndNeverChangesSavedMinimapPreferences() {
    var caves = new WorldMapCaves();
    var settings = new MapSettings();
    settings.caveMode = 2; settings.caveTop = 100;
    settings.caveAbove = 3; settings.caveBelow = 7;
    caves.select(64); var first = caves.window(settings, 80);
    caves.select(65); var second = caves.window(settings, 80);
    assertEquals(first.layer(), second.layer());
    assertNotEquals(first.context(), second.context());
    assertEquals(68, second.top()); assertEquals(58, second.bottom());
    caves.select(-1); assertEquals(0, caves.window(settings, 80).reference());
    caves.select(128); assertEquals(127, caves.window(settings, 80).reference());
    caves.reset(); assertEquals(80, caves.window(settings, 80).reference());
    assertEquals(100, settings.caveTop); assertEquals(2, settings.caveMode);
  }
}
