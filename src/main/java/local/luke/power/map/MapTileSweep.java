package local.luke.power.map;

/** Constant-memory square spiral. Each lattice cell occurs once, starting at the view center. */
public final class MapTileSweep {
  public record Cell(int x, int z) {}
  private MapTileSweep() {}
  /** The fallback/minimap coverage is bounded to1024 tiles, including very thin windows. */
  public static java.util.List<Cell> nearest(MapCoverage.Bounds bounds, int centerX, int centerZ) {
    if (bounds.count() > 1024) throw new IllegalArgumentException("Unbounded map coverage");
    var cells = new java.util.ArrayList<Cell>((int)Math.max(0, bounds.count()));
    for (int z = bounds.minZ(); z <= bounds.maxZ(); z++)
      for (int x = bounds.minX(); x <= bounds.maxX(); x++) cells.add(new Cell(x, z));
    cells.sort(java.util.Comparator.comparingLong(cell -> {
      long dx = (long)cell.x - centerX, dz = (long)cell.z - centerZ;
      return dx * dx + dz * dz;
    }));
    return cells;
  }
  public static Cell cell(long index) {
    if (index < 0) throw new IllegalArgumentException("Negative sweep index");
    if (index == 0) return new Cell(0, 0);
    int radius = (int) ((Math.sqrt(index) + 1) / 2);
    long edge = radius * 2L, offset = index - (edge - 1) * (edge - 1);
    if (offset < edge) return new Cell((int) (-radius + 1 + offset), -radius);
    if (offset < edge * 2) return new Cell(radius, (int) (-radius + 1 + offset - edge));
    if (offset < edge * 3) return new Cell((int) (radius - 1 - (offset - edge * 2)), radius);
    return new Cell(-radius, (int) (radius - 1 - (offset - edge * 3)));
  }
}
