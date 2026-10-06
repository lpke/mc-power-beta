package local.luke.power.autowalk;

public final class ReachDistance {
  private ReachDistance() {}
  public static double toBox(double x, double y, double z, double minX, double minY, double minZ,
      double maxX, double maxY, double maxZ) {
    double dx = x - Math.max(minX, Math.min(maxX, x));
    double dy = y - Math.max(minY, Math.min(maxY, y));
    double dz = z - Math.max(minZ, Math.min(maxZ, z));
    double squared = dx*dx + dy*dy + dz*dz;
    return Double.isFinite(squared) ? squared : Double.POSITIVE_INFINITY;
  }
}
