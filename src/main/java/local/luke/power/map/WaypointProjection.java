package local.luke.power.map;

import java.nio.FloatBuffer;

/** Projects a relative world point using one captured camera transform, retaining subpixel precision. */
public final class WaypointProjection {
  public record Point(double x, double y) {}
  private WaypointProjection() {}

  public static Point project(double x, double y, double z, FloatBuffer model, FloatBuffer projection) {
    double mx = row(model, 0, x, y, z, 1), my = row(model, 1, x, y, z, 1),
        mz = row(model, 2, x, y, z, 1), mw = row(model, 3, x, y, z, 1);
    double w = row(projection, 3, mx, my, mz, mw);
    if (!Double.isFinite(w) || w <= 1e-9) return null;
    double depth = row(projection, 2, mx, my, mz, mw) / w;
    if (!Double.isFinite(depth) || depth < -1) return null;
    double px = row(projection, 0, mx, my, mz, mw) / w,
        py = row(projection, 1, mx, my, mz, mw) / w;
    return Double.isFinite(px) && Double.isFinite(py) ? new Point((px + 1) / 2, (py + 1) / 2) : null;
  }

  private static double row(FloatBuffer matrix, int row, double x, double y, double z, double w) {
    return matrix.get(row) * x + matrix.get(row + 4) * y + matrix.get(row + 8) * z + matrix.get(row + 12) * w;
  }
}
