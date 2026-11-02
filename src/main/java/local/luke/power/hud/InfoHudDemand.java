package local.luke.power.hud;

/** Only visible consumers request sampled values; external clocks remain independent of custom HUD. */
final class InfoHudDemand {
  static final int COORDINATES = 1, DIRECTION = 2, BIOME = 4, TIME = 8, DAY = 16, CHUNK = 32, FPS = 64;
  private InfoHudDemand() {}

  static boolean visible(InfoHudSettings.Cluster cluster, boolean hiddenHud, boolean debug) {
    return !hiddenHud && cluster.enabled && cluster.opacity > 0 && !(debug && cluster.hideWithDebug);
  }

  static int fields(InfoHudSettings.Cluster cluster) {
    return (cluster.coordinates ? COORDINATES : 0) | (cluster.direction ? DIRECTION : 0)
        | (cluster.biome ? BIOME : 0) | (cluster.timeOfDay ? TIME : 0)
        | (cluster.day ? DAY : 0) | (cluster.chunk ? CHUNK : 0) | (cluster.fps ? FPS : 0);
  }

  static int of(InfoHudSettings settings, boolean hiddenHud, boolean debug, boolean externalClock) {
    int fields = externalClock ? TIME : 0;
    if (settings.enabled) for (var cluster : settings.clusters)
      if (visible(cluster, hiddenHud, debug)) fields |= fields(cluster);
    return fields;
  }
}
