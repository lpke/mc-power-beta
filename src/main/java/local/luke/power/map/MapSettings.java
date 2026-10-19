package local.luke.power.map;

/** Global preferences only. Explored terrain and waypoints live in separate world data. */
public final class MapSettings {
  public boolean minimap = true, worldMap = true, rotate = true, circular, hideWithDebug = true;
  public int size = 108, corner = 1, offsetX = 6, offsetY = 6;
  public double opacity = 1, zoom = 1;
  public boolean coordinates = true, biome = true, clock, compass = true;
  public int colourMode, caveMode = 1, caveTop = 64, roofSize = 3;
  public boolean heightShading = true, slopeShading = true, caveLighting = true;
  public boolean radar = true, players = true, hostile = true, animals = true, items, otherEntities;
  public boolean entityNames, entityHeights = true;
  public int radarHeight = 24, radarLimit = 256;
  public boolean waypoints = true,
      worldWaypoints = true,
      waypointNames = true,
      waypointDistances = true;
  public int waypointDistance = 1000;
  public boolean deathpoints = true, keepOldDeaths = true;
  public boolean chunkGrid, slimeChunks, lightOverlay, footsteps = true, pauseMap;
  public int columnsPerTick = 512, cacheTiles = 256, uploadsPerFrame = 4, refreshSeconds = 5;

  public void validate() {
    range(size, 64, 240);
    range(corner, 0, 3);
    range(offsetX, 0, 500);
    range(offsetY, 0, 500);
    finite(opacity, .1, 1);
    finite(zoom, .25, 4);
    range(colourMode, 0, 1);
    range(caveMode, 0, 2);
    range(caveTop, 1, 127);
    range(roofSize, 1, 5);
    range(radarHeight, 1, 128);
    range(radarLimit, 16, 1024);
    range(waypointDistance, 0, 1000000);
    range(columnsPerTick, 32, 4096);
    range(cacheTiles, 64, 1024);
    range(uploadsPerFrame, 1, 16);
    range(refreshSeconds, 1, 60);
  }

  private static void range(int v, int min, int max) {
    if (v < min || v > max)
      throw new IllegalArgumentException("Map value outside " + min + "–" + max);
  }

  private static void finite(double v, double min, double max) {
    if (!Double.isFinite(v) || v < min || v > max)
      throw new IllegalArgumentException("Invalid map scale or opacity");
  }

  public int style() {
    return java.util.Objects.hash(colourMode, heightShading, slopeShading, caveLighting);
  }
}
