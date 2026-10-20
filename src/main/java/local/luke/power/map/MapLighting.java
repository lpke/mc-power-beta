package local.luke.power.map;

/** Render-only daylight and radar colors. Stored terrain and world light stay unchanged. */
public final class MapLighting {
  private MapLighting() {}

  public static double shade(int blockLight, boolean cave, boolean caveLighting, boolean fullBright, int skyDarkness) {
    if (fullBright || cave && !caveLighting) return 1;
    int light = cave ? blockLight : Math.max(blockLight, 15 - Math.max(0, Math.min(11, skyDarkness)));
    return .35 + Math.max(0, Math.min(15, light)) * .65 / 15;
  }

  public static int radarColor(int color, double heightDifference, MapSettings settings) {
    if (!settings.radarDimming) return color;
    double brightness = 1 - .65 * Math.min(1, Math.abs(heightDifference) / settings.radarDimDistance);
    return MapMath.shade(color, brightness);
  }
}
