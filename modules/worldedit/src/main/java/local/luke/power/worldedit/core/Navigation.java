package local.luke.power.worldedit.core;

/** Bounded navigation planning. Never loads chunks or modifies the world. */
public final class Navigation {
  public interface Terrain {
    boolean clear(Pos feet);

    boolean floor(Pos feet);

    boolean loaded(Pos pos);
  }

  private final Terrain terrain;

  public Navigation(Terrain terrain) {
    this.terrain = terrain;
  }

  public boolean clear(Pos p) {
    return p.valid() && p.y() >= 1 && p.y() <= 126 && terrain.loaded(p) && terrain.clear(p);
  }

  public boolean safe(Pos p) {
    return clear(p) && terrain.floor(p);
  }

  public Pos up(Pos from, int distance) {
    Pos result = from.add(new Pos(0, distance, 0));
    if (!clear(result)) throw new IllegalArgumentException("No room at that height.");
    for (int y = from.y() + 1; y <= result.y(); y++)
      if (!clear(new Pos(from.x(), y, from.z())))
        throw new IllegalArgumentException("A ceiling blocks the way. Try /ascend.");
    return result;
  }

  public Pos floor(Pos from, int direction, int levels) {
    for (int y = from.y() + direction; y >= 1 && y <= 126; y += direction) {
      Pos p = new Pos(from.x(), y, from.z());
      if (safe(p) && --levels == 0) return p;
    }
    throw new IllegalArgumentException("No safe floor found.");
  }

  public Pos ceiling(Pos from, int clearance) {
    for (int y = from.y() + 1; y <= 126; y++) {
      Pos p = new Pos(from.x(), y, from.z());
      if (!terrain.loaded(p)) break;
      if (!clear(p)) {
        Pos result = new Pos(from.x(), y - 1 - clearance, from.z());
        if (result.y() <= from.y() || !clear(result)) break;
        return result;
      }
    }
    throw new IllegalArgumentException("No reachable ceiling found.");
  }

  public Pos unstuck(Pos from) {
    Pos best = null;
    long nearest = Long.MAX_VALUE;
    for (int x = -4; x <= 4; x++)
      for (int z = -4; z <= 4; z++)
        for (int y = 1; y <= 126; y++) {
          long dy = y - from.y(), distance = x * x + z * z + dy * dy;
          if (distance >= nearest) continue;
          Pos p = new Pos(from.x() + x, y, from.z() + z);
          if (safe(p)) {
            best = p;
            nearest = distance;
          }
        }
    if (best == null)
      throw new IllegalArgumentException("No safe position in nearby loaded chunks.");
    return best;
  }
}
