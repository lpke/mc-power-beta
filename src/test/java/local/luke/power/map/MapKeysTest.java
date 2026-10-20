package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import local.luke.power.config.Catalog;
import org.junit.jupiter.api.Test;

class MapKeysTest {
  @Test void bindingsKeepTheirDefaultsWithAnIndependentTabHoldAndCatalogedActions() {
    Map<String, Integer> original = Map.of("powerbeta.map", 50, "powerbeta.waypoint", 48,
        "powerbeta.waypoints", 22, "powerbeta.enlargeMap", 44, "powerbeta.mapZoomIn", 23,
        "powerbeta.mapZoomOut", 24, "powerbeta.minimap", 0, "powerbeta.radarIcons", 15);
    Set<String> added = Set.of("powerbeta.toggleWorldWaypoints", "powerbeta.toggleMapWaypoints",
        "powerbeta.toggleMapRadar", "powerbeta.toggleMapCaves", "powerbeta.toggleMapGrid",
        "powerbeta.clearMapDestination", "powerbeta.mapSettings", "powerbeta.mapZoomReset");
    Set<String> found = new HashSet<>();
    for (var key : MapKeys.all()) {
      assertTrue(found.add(key.translationKey), key.translationKey);
      assertEquals(original.getOrDefault(key.translationKey, 0).intValue(), key.code);
      assertTrue(original.containsKey(key.translationKey) || added.contains(key.translationKey));
      assertFalse(Catalog.text(Catalog.metadata("keys." + key.translationKey), "label", "").isBlank());
    }
    assertEquals(16, found.size()); assertTrue(found.containsAll(added));
  }

  @Test void viewAvailabilityNeverClaimsHiddenZoomActionsAndKeepsRecoveryControls() {
    MapSettings settings = new MapSettings(); settings.minimap = false; settings.worldMap = false;
    for (String id : List.of("powerbeta.map", "powerbeta.enlargeMap", "powerbeta.mapZoomIn",
        "powerbeta.mapZoomOut", "powerbeta.mapZoomReset", "powerbeta.toggleMapWaypoints",
        "powerbeta.toggleMapRadar", "powerbeta.toggleMapCaves", "powerbeta.toggleMapGrid", "powerbeta.clearMapDestination", "powerbeta.radarIcons"))
      assertFalse(MapKeys.available(id, settings), id);
    for (String id : List.of("powerbeta.minimap", "powerbeta.mapSettings", "powerbeta.toggleWorldWaypoints",
        "powerbeta.waypoint", "powerbeta.waypoints", "key.forward")) assertTrue(MapKeys.available(id, settings), id);
    settings.worldMap = true;
    assertTrue(MapKeys.available("powerbeta.map", settings));
    assertTrue(MapKeys.available("powerbeta.toggleMapRadar", settings));
    assertTrue(MapKeys.available("powerbeta.radarIcons", settings));
    settings.radar = false;
    assertFalse(MapKeys.available("powerbeta.radarIcons", settings));
    settings.radar = true;
    assertFalse(MapKeys.available("powerbeta.mapZoomIn", settings));
    settings.minimap = true;
    assertTrue(MapKeys.available("powerbeta.mapZoomIn", settings));
  }

  @Test void waypointTogglesStayIndependentAndEverySwitchCanBeReenabled() {
    MapSettings settings = new MapSettings();
    assertTrue(MapKeys.change("powerbeta.toggleWorldWaypoints", settings));
    assertFalse(settings.worldWaypoints); assertTrue(settings.waypoints);
    assertTrue(MapKeys.change("powerbeta.toggleMapWaypoints", settings));
    assertFalse(settings.waypoints); assertFalse(settings.worldWaypoints);
    assertTrue(MapKeys.change("powerbeta.toggleWorldWaypoints", settings));
    assertTrue(settings.worldWaypoints); assertFalse(settings.waypoints);
    assertTrue(MapKeys.change("powerbeta.toggleMapWaypoints", settings)); assertTrue(settings.waypoints);
    assertTrue(MapKeys.change("powerbeta.toggleMapRadar", settings)); assertFalse(settings.radar);
    assertTrue(MapKeys.change("powerbeta.toggleMapRadar", settings)); assertTrue(settings.radar);
    assertTrue(MapKeys.change("powerbeta.toggleMapGrid", settings)); assertTrue(settings.chunkGrid);
    assertTrue(MapKeys.change("powerbeta.toggleMapGrid", settings)); assertFalse(settings.chunkGrid);
    settings.minimap = false; settings.worldMap = false;
    assertTrue(MapKeys.change("powerbeta.toggleWorldWaypoints", settings));
    assertFalse(settings.worldWaypoints);
    assertTrue(MapKeys.change("powerbeta.minimap", settings)); assertTrue(settings.minimap);
  }

  @Test void cavesCycleAllModesAndZoomActionsRespectTheirBoundsAndAvailability() {
    MapSettings settings = new MapSettings(); settings.caveMode = 0;
    for (int expected : new int[] {1, 2, 0}) {
      assertTrue(MapKeys.change("powerbeta.toggleMapCaves", settings)); assertEquals(expected, settings.caveMode);
    }
    settings.zoom = 3;
    MapKeys.change("powerbeta.mapZoomIn", settings); assertEquals(4, settings.zoom);
    MapKeys.change("powerbeta.mapZoomIn", settings); assertEquals(.25, settings.zoom);
    settings.zoom = .3;
    MapKeys.change("powerbeta.mapZoomOut", settings); assertEquals(.25, settings.zoom);
    MapKeys.change("powerbeta.mapZoomOut", settings); assertEquals(4, settings.zoom);
    MapKeys.change("powerbeta.mapZoomReset", settings); assertEquals(1, settings.zoom);
    settings.minimap = false; settings.zoom = 3;
    for (String id : List.of("powerbeta.mapZoomIn", "powerbeta.mapZoomOut", "powerbeta.mapZoomReset"))
      assertFalse(MapKeys.change(id, settings), id);
    assertEquals(3, settings.zoom); assertDoesNotThrow(settings::validate);
    assertFalse(MapKeys.change("powerbeta.clearMapDestination", settings));
    assertFalse(MapKeys.change("powerbeta.mapSettings", settings));
    assertFalse(MapKeys.change("powerbeta.radarIcons", settings));
  }
}
