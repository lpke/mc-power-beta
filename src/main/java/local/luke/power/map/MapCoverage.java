package local.luke.power.map;

/** Tight world bounds and a complete tile level selected within the view's work budget. */
public final class MapCoverage {
  public record Bounds(int level, int minX, int maxX, int minZ, int maxZ) {
    public long count() {
      return (long) (maxX - minX + 1) * (maxZ - minZ + 1);
    }
  }

  private MapCoverage() {}

  public static Bounds select(MapProjection view, boolean circle, int budget) {
    int level = MapMath.level(view.scale);
    Bounds bounds = bounds(view, circle, level);
    while (bounds.count() > budget && level < MapMath.MAX_LEVEL)
      bounds = bounds(view, circle, ++level);
    return bounds;
  }

  private static Bounds bounds(MapProjection view, boolean circle, int level) {
    double cos = Math.abs(Math.cos(view.angle)), sin = Math.abs(Math.sin(view.angle));
    double dx = (circle ? view.width : cos * view.width + sin * view.height) / 2 / view.scale;
    double dz = (circle ? view.height : sin * view.width + cos * view.height) / 2 / view.scale;
    double minX = Math.max(-MapMath.BORDER, view.x - dx),
        maxX = Math.min(MapMath.BORDER, view.x + dx);
    double minZ = Math.max(-MapMath.BORDER, view.z - dz),
        maxZ = Math.min(MapMath.BORDER, view.z + dz);
    if (minX > maxX || minZ > maxZ) return new Bounds(level, 0, -1, 0, -1);
    int span = MapMath.SIZE << level;
    return new Bounds(
        level,
        (int) Math.floor(minX / span),
        (int) Math.floor(maxX / span),
        (int) Math.floor(minZ / span),
        (int) Math.floor(maxZ / span));
  }
}
