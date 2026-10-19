package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MapActivityTest {
  @Test void disabledViewsDoNotKeepTheirTerrainRadarOrPathWorkAlive() {
    var settings = new MapSettings();
    settings.minimap = settings.worldMap = false;
    settings.worldWaypoints = settings.deathpoints = false;
    var activity = MapActivity.of(settings, false);
    assertFalse(activity.terrain());
    assertFalse(activity.radar());
    assertFalse(activity.path());
    assertFalse(activity.waypoints(), "Map-only waypoint visibility needs an enabled map");
  }
  @Test void eitherEnabledMapOwnsTheSharedTerrainWork() {
    for (boolean minimap : new boolean[] {false, true}) {
      var settings = new MapSettings();
      settings.minimap = minimap; settings.worldMap = !minimap;
      var activity = MapActivity.of(settings, false);
      assertTrue(activity.terrain());
      assertTrue(activity.radar());
      assertTrue(activity.path());
      settings.radar = settings.footsteps = false;
      activity = MapActivity.of(settings, false);
      assertFalse(activity.radar()); assertFalse(activity.path());
    }
  }
  @Test void independentWaypointFeaturesAndExplicitEditorsDoNotRequireTerrain() {
    var settings = new MapSettings();
    settings.minimap = settings.worldMap = settings.worldWaypoints = settings.deathpoints = false;
    assertTrue(MapActivity.of(settings, true).waypoints());
    settings.worldWaypoints = true;
    assertTrue(MapActivity.of(settings, false).waypoints());
    assertFalse(MapActivity.of(settings, false).terrain());
    settings.worldWaypoints = false; settings.deathpoints = true;
    assertTrue(MapActivity.of(settings, false).waypoints());
    assertFalse(MapActivity.of(settings, false).terrain());
  }
  @Test void radarWithNoIncludedEntityTypesDoesNotScanEntities() {
    var settings = new MapSettings();
    settings.players = settings.hostile = settings.animals = settings.items = settings.otherEntities = false;
    assertFalse(MapActivity.of(settings, false).radar());
  }
}
