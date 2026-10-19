package local.luke.power.map;

/** Edge-arrow controls apply only to the minimap, independently for each waypoint kind. */
public final class MinimapArrows {
  public record Style(boolean visible, int color, double opacity, double scale) {
    public double inset() { return 2 + 6.5 * scale; }
  }
  private MinimapArrows() {}
  public static boolean destination(Waypoint point) { return "temporary-destination".equals(point.id); }
  public static Style style(Waypoint point, MapSettings settings, boolean minimap) {
    boolean destination = destination(point);
    boolean enabled = destination ? settings.minimapDestinationArrow
        : point.death ? settings.minimapDeathpointArrows : settings.minimapWaypointArrows;
    return new Style(minimap && settings.waypoints && enabled,
        destination ? settings.destinationArrowColor : point.colour,
        destination ? settings.destinationArrowOpacity : 1, settings.minimapArrowSize / 100d);
  }
}
