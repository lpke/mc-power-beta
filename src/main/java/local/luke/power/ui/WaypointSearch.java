package local.luke.power.ui;

import local.luke.power.map.MapDimensions;
import local.luke.power.map.Waypoint;

final class WaypointSearch {
  private WaypointSearch() {}

  static int score(String query, Waypoint point) {
    return FuzzySearch.score(query, point.name, point.abbreviation() + " " + point.set + " "
        + point.coordinates() + " " + MapDimensions.name(point.dimension));
  }
}
