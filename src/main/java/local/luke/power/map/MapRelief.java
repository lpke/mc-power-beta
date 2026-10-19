package local.luke.power.map;

/** Quantised north-west surface lighting, calculated before map levels are reduced. */
final class MapRelief {
  private MapRelief() {}

  static int colour(int rgb, int height, int north, int northwest, int dimension) {
    double vertical = height - north, diagonal = height - northwest;
    double direct = 0;
    if (vertical >= 0) {
      double cosine =
          (1 + vertical)
              / Math.sqrt(
                  2 * ((vertical - diagonal) * (vertical - diagonal) + 1 + vertical * vertical));
      direct = cosine >= 1 ? 2d / 3 : Math.ceil(cosine * 10) / 10 * (2d / 3) * .88388;
    }
    double ambient = .5 + direct;
    return channel(rgb >> 16, ambient + (dimension == -1 ? .2 : .1036)) << 16
        | channel(rgb >> 8, ambient + (dimension == -1 ? 0 : .1356)) << 8
        | channel(rgb, ambient + (dimension == -1 ? 0 : .2));
  }

  private static int channel(int colour, double light) {
    return Math.min(255, Math.max(0, (int) ((colour & 255) * light)));
  }
}
