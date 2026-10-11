package local.luke.power.worldedit.core;

import java.util.ArrayList;
import java.util.List;

/** Visits every block crossed by a bounded ray, including very short corner crossings. */
public final class BlockRay {
  private BlockRay() {}

  public static List<Pos> cells(double x, double y, double z,
      double dx, double dy, double dz, double range) {
    double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
    if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
        || Math.abs(x) > Integer.MAX_VALUE - 256d || Math.abs(y) > Integer.MAX_VALUE - 256d
        || Math.abs(z) > Integer.MAX_VALUE - 256d
        || !Double.isFinite(length) || length == 0 || !Double.isFinite(range)
        || range < 0 || range > 256) throw new IllegalArgumentException("Invalid navigation ray.");
    dx /= length; dy /= length; dz /= length;
    int bx = (int) Math.floor(x), by = (int) Math.floor(y), bz = (int) Math.floor(z);
    int sx = (int) Math.signum(dx), sy = (int) Math.signum(dy), sz = (int) Math.signum(dz);
    double tx = boundary(x, bx, dx), ty = boundary(y, by, dy), tz = boundary(z, bz, dz);
    double ix = sx == 0 ? Double.POSITIVE_INFINITY : Math.abs(1 / dx);
    double iy = sy == 0 ? Double.POSITIVE_INFINITY : Math.abs(1 / dy);
    double iz = sz == 0 ? Double.POSITIVE_INFINITY : Math.abs(1 / dz);
    List<Pos> result = new ArrayList<>();
    for (;;) {
      result.add(new Pos(bx, by, bz));
      double next = Math.min(tx, Math.min(ty, tz));
      if (next > range) return result;
      if (tx == next) { bx += sx; tx += ix; }
      if (ty == next) { by += sy; ty += iy; }
      if (tz == next) { bz += sz; tz += iz; }
    }
  }

  private static double boundary(double origin, int cell, double direction) {
    if (direction == 0) return Double.POSITIVE_INFINITY;
    return ((direction > 0 ? cell + 1d : cell) - origin) / direction;
  }
}
