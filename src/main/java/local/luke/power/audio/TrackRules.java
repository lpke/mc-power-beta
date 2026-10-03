package local.luke.power.audio;

import java.util.Locale;

/** Preserves the existing dimension and biome filename conventions. */
public final class TrackRules {
  private TrackRules() {}

  public static boolean eligible(String name, int dimension, String biome) {
    if (!name.contains("-specific.")) return true;
    return dimensionSpecific(name, dimension)
        || biome != null && name.contains("-" + biome.toLowerCase(Locale.ROOT) + "-");
  }

  public static boolean dimensionSpecific(String name, int dimension) {
    return name.contains("-specific.")
        && (name.contains("-level" + dimension + "-")
            || dimension == 0 && name.contains("-overworld-")
            || dimension == -1 && name.contains("-nether-"));
  }
}
