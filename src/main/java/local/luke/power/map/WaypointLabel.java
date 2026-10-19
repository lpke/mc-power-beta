package local.luke.power.map;

/** Stable screen-space aiming and a bounded, continuous distance response. */
public record WaypointLabel(String name, String distance, double scale) {
  public static WaypointLabel of(Waypoint point, MapSettings settings, boolean aimed,
      double cameraDistance, double horizontalDistance) {
    String name = MinimapArrows.destination(point) ? point.x + "," + point.y + "," + point.z
        : point.death ? aimed ? WaypointTimes.detail(point.created) : ""
        : !settings.waypointNames ? "" : aimed ? point.name : point.abbreviation();
    String distance = settings.waypointDistances ? Math.round(horizontalDistance) + "m" : "";
    if (!Double.isFinite(cameraDistance)) cameraDistance = Double.MAX_VALUE;
    double response = !settings.waypointDistanceScaling ? 1
        : Math.max(.65, Math.min(1, Math.pow(32 / Math.max(32, cameraDistance), .12)));
    return new WaypointLabel(name, distance, settings.waypointTextSize / 100d * response);
  }
}
