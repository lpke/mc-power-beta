package local.luke.power.render;

/** Bounded radial lighting and section selection, independent of game or GL state. */
public final class HeldLightMath {
  public static final double RADIUS = 7.75, PREPARE_RADIUS = 15.75;
  private HeldLightMath() {}

  public static double sectionDistanceSquared(double x, double y, double z, int sx, int sy, int sz) {
    double dx = Math.max(Math.max(sx - x, x - (sx + 16.0)), 0);
    double dy = Math.max(Math.max(sy - y, y - (sy + 16.0)), 0);
    double dz = Math.max(Math.max(sz - z, z - (sz + 16.0)), 0);
    return dx * dx + dy * dy + dz * dz;
  }

  public static float brightness(float[] table, int emission, double distanceSquared) {
    if (emission <= 0 || emission > 15 || !Double.isFinite(distanceSquared) || distanceSquared < 0
        || distanceSquared >= RADIUS * RADIUS) return 0;
    float level = (float)(emission * (1 - Math.sqrt(distanceSquared) / RADIUS));
    int low = Math.min(15, Math.max(0, (int)level)), high = Math.min(15, low + 1);
    return table[low] + (table[high] - table[low]) * (level - low);
  }

  public static boolean finiteSource(double x, double y, double z) {
    return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
        && Math.abs(x) <= 32000000 && Math.abs(z) <= 32000000 && y > -RADIUS && y < 128 + RADIUS;
  }
}
