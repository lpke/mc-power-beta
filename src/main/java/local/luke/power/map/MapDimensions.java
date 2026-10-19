package local.luke.power.map;

/** Native dimension names without changing stored dimension IDs. */
public final class MapDimensions {
  private MapDimensions() {}

  public static String name(int id) {
    return switch (id) {
      case 0 -> "Overworld";
      case -1 -> "Nether";
      default -> "Dimension " + id;
    };
  }
}
