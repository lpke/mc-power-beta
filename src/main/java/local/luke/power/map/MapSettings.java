package local.luke.power.map;

/** Global preferences only. Explored terrain and waypoints live in separate world data. */
public final class MapSettings {
  public boolean minimap = true, worldMap = true, rotate = true, circular = true, hideWithDebug = true;
  public int size = 108, corner = 1, offsetX = 6, offsetY = 6;
  public double opacity = 1, zoom = 1;
  public double[] zoomSteps = {.25, .5, 1.25, 2.5, 4};
  public boolean coordinates = true, biome = true, clock, compass = true;
  public int directionTextSize = 100, belowTextSize = 100, minimapArrowSize = 100;
  public int minimapWaypointTextSize = 100, minimapWaypointSize = 100;
  public int directionTextColor = 0xffffff;
  public double directionTextOpacity = 1, minimapArrowOpacity = 1;
  public boolean minimapWaypointArrows = true, minimapDestinationArrow = true, minimapDeathpointArrows = true;
  public int destinationArrowColor = 0x00ecff;
  public double destinationArrowOpacity = 1;
  public int colourMode, caveMode = 1, caveTop = 64, roofSize = 3;
  public int caveAbove = 10, caveBelow = 10;
  public int pathColor = 0xffff99, playerColor = 0xf13c3c, playerIcon, playerSize = 100;
  public double pathOpacity = .7, playerOpacity = 1;
  public boolean automaticDetail = true, mapSmoothing;
  public int mapOpenColumns = 1024, mapOpenUploads = 8;
  public double mapOpenBudget = 2;
  public boolean heightShading = true, slopeShading = true, caveLighting = true;
  public boolean radar = true, players = true, hostile = true, animals = true, items, otherEntities = true;
  public boolean entityNames, entityHeights = true;
  public boolean worldMapRadarIcons;
  public int radarPlayersColor = 0xffffff, radarHostileColor = 0xff6655,
      radarAnimalsColor = 0xffff55, radarItemsColor = 0xff5555,
      radarVehiclesColor = 0xcc66ff, radarProjectilesColor = 0xcc66ff,
      radarOtherColor = 0xcc66ff;
  /** -1 uses the matching entity category color. RGB zero remains custom black. */
  public int radarCreeperColor = -1, radarSkeletonColor = -1, radarSpiderColor = -1,
      radarGiantColor = -1, radarZombieColor = -1, radarSlimeColor = -1,
      radarGhastColor = -1, radarZombiePigmanColor = -1,
      radarPigColor = -1, radarSheepColor = -1, radarCowColor = -1,
      radarChickenColor = -1, radarSquidColor = -1, radarWolfColor = -1;
  public int radarHeight = 24, radarLimit = 256, radarIconSize = 50;
  public boolean radarDimming = true;
  public int radarDimDistance = 24;
  public int lightRadius = 64;
  public boolean waypoints = true,
      worldWaypoints = true,
      waypointNames = true,
      waypointDistances = true;
  public int waypointDistance = 1000;
  public int waypointTextSize = 75;
  public int worldMapWaypointSize = 85, waypointBobbing = 1;
  public boolean waypointDistanceScaling = true;
  public boolean deathpoints = true, keepOldDeaths = true;
  public boolean chunkGrid, slimeChunks, lightOverlay, footsteps, pauseMap;
  public int columnsPerTick = 512,
      cacheTiles = 256,
      uploadsPerFrame = 4,
      refreshSeconds = 5,
      updateRate = 20;

  public void validate() {
    range(waypointTextSize, 50, 150);
    range(worldMapWaypointSize, 50, 150);
    range(waypointBobbing, 0, 1);
    range(size, 64, 240);
    range(corner, 0, 3);
    range(offsetX, 0, 500);
    range(offsetY, 0, 500);
    finite(opacity, .1, 1);
    finite(zoom, .25, 4);
    MinimapZoomSteps.validate(zoomSteps);
    range(directionTextSize, 50, 200);
    range(belowTextSize, 50, 200);
    range(minimapArrowSize, 5, 200);
    range(minimapWaypointTextSize, 50, 200);
    range(minimapWaypointSize, 50, 200);
    range(directionTextColor, 0, 0xffffff);
    finite(directionTextOpacity, 0, 1);
    finite(minimapArrowOpacity, 0, 1);
    range(destinationArrowColor, 0, 0xffffff);
    finite(destinationArrowOpacity, 0, 1);
    range(colourMode, 0, 1);
    range(caveMode, 0, 2);
    range(caveTop, 1, 127);
    range(roofSize, 1, 5);
    range(caveAbove, 0, 64);
    range(caveBelow, 0, 64);
    range(pathColor, 0, 0xffffff);
    range(playerColor, 0, 0xffffff);
    range(playerIcon, 0, 3);
    range(playerSize, 50, 200);
    finite(pathOpacity, 0, 1);
    finite(playerOpacity, 0, 1);
    range(mapOpenColumns, 32, 8192);
    range(mapOpenUploads, 1, 32);
    finite(mapOpenBudget, .5, 8);
    range(radarIconSize, 10, 200);
    range(radarHeight, 1, 128);
    for (int color : new int[] {radarPlayersColor, radarHostileColor, radarAnimalsColor,
        radarItemsColor, radarVehiclesColor, radarProjectilesColor, radarOtherColor})
      range(color, 0, 0xffffff);
    for (int color : new int[] {radarCreeperColor, radarSkeletonColor, radarSpiderColor,
        radarGiantColor, radarZombieColor, radarSlimeColor, radarGhastColor,
        radarZombiePigmanColor, radarPigColor, radarSheepColor, radarCowColor,
        radarChickenColor, radarSquidColor, radarWolfColor}) range(color, -1, 0xffffff);
    range(radarLimit, 16, 1024);
    range(radarDimDistance, 1, 128);
    range(lightRadius, 8, 128);
    range(waypointDistance, 0, 1000000);
    range(columnsPerTick, 32, 4096);
    range(cacheTiles, 64, 1024);
    range(uploadsPerFrame, 1, 16);
    range(refreshSeconds, 1, 60);
    range(updateRate, 1, 20);
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
    return style(false);
  }

  public boolean caveLighting(boolean fullBright) { return caveLighting && !fullBright; }

  public int mobColor(String nativeEntityId, int categoryColor) {
    if (nativeEntityId == null) return categoryColor;
    int custom = switch (nativeEntityId) {
      case "Creeper" -> radarCreeperColor;
      case "Skeleton" -> radarSkeletonColor;
      case "Spider" -> radarSpiderColor;
      case "Giant" -> radarGiantColor;
      case "Zombie" -> radarZombieColor;
      case "Slime" -> radarSlimeColor;
      case "Ghast" -> radarGhastColor;
      case "PigZombie" -> radarZombiePigmanColor;
      case "Pig" -> radarPigColor;
      case "Sheep" -> radarSheepColor;
      case "Cow" -> radarCowColor;
      case "Chicken" -> radarChickenColor;
      case "Squid" -> radarSquidColor;
      case "Wolf" -> radarWolfColor;
      default -> -1;
    };
    return custom < 0 ? categoryColor : custom;
  }

  public int radarColorStyle() {
    return java.util.Objects.hash(radarPlayersColor, radarHostileColor, radarAnimalsColor,
        radarItemsColor, radarVehiclesColor, radarProjectilesColor, radarOtherColor,
        radarCreeperColor, radarSkeletonColor, radarSpiderColor, radarGiantColor,
        radarZombieColor, radarSlimeColor, radarGhastColor, radarZombiePigmanColor,
        radarPigColor, radarSheepColor, radarCowColor, radarChickenColor, radarSquidColor,
        radarWolfColor);
  }

  /** Runtime brightness changes rendered colors without changing the saved preference. */
  public int style(boolean fullBright) {
    return java.util.Objects.hash(colourMode, heightShading, slopeShading, caveLighting(fullBright));
  }
}
