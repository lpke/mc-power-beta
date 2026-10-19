package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import local.luke.power.map.Waypoint;
import org.junit.jupiter.api.Test;

class WaypointSearchTest {
  @Test void waypointsUseSharedTypoInitialsAndAccentMatching() {
    var point = new Waypoint(); point.name = "Émerald Base";
    for (String query : new String[] {"emreald", "emerld", "eb", "emerald"})
      assertTrue(WaypointSearch.score(query, point) < FuzzySearch.NONE, query);
    assertEquals(FuzzySearch.NONE, WaypointSearch.score("emerald village", point));
  }

  @Test void abbreviationSetCoordinatesAndDimensionAreSearchableContext() {
    var point = new Waypoint(); point.name = "Home"; point.abbreviation = "HQ";
    point.set = "Mining camps"; point.x = -42; point.y = 70; point.z = 250; point.dimension = -1;
    for (String query : new String[] {"hq", "mining", "-42 70 250", "nether", "home mining"})
      assertTrue(WaypointSearch.score(query, point) < FuzzySearch.NONE, query);
    var named = point.copy(); named.name = "Mining";
    assertTrue(WaypointSearch.score("mining", named) < WaypointSearch.score("mining", point));
  }
}
