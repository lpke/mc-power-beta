package local.luke.power.map;

/** Independent feature switches determine which background work has a consumer. */
public record MapActivity(boolean terrain, boolean radar, boolean path, boolean waypoints) {
  public static MapActivity of(MapSettings settings, boolean explicitWaypoints) {
    boolean terrain = settings.minimap || settings.worldMap;
    boolean entityTypes = settings.players || settings.hostile || settings.animals || settings.items || settings.otherEntities;
    return new MapActivity(terrain, terrain && settings.radar && entityTypes,
        terrain && settings.footsteps,
        terrain && settings.waypoints || settings.worldWaypoints || settings.deathpoints || explicitWaypoints);
  }
}
