package local.luke.power.map;

/**
 * Texture allocation and painted coverage differ: unused power-of-two padding requests no tiles.
 */
public record MapBufferLayout(int side, int coverage) {
  public static MapBufferLayout create(double width, double pixelsPerUnit, int hardwareLimit) {
    int maximum = Math.min(2048, hardwareLimit);
    double needed = width * pixelsPerUnit * Math.sqrt(2) + 8;
    if (needed > maximum || maximum < 256) return null;
    int coverage =
        (int) Math.min(maximum, Math.ceil(needed + Math.min(64, width * pixelsPerUnit * .25)));
    int side = 256;
    while (side < coverage) side *= 2;
    if (side > maximum) return null;
    return new MapBufferLayout(side, coverage);
  }
}
