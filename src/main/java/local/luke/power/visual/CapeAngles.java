package local.luke.power.visual;

/** Bounds the rendered cape only; player and vehicle movement remain untouched. */
public final class CapeAngles {
  private CapeAngles() {}
  public static float pitch(float angle) { return Float.isFinite(angle) ? Math.max(0, Math.min(90, angle)) : 0; }
  public static float sideways(float angle) { return 0; }

  /** One angle for the whole gait, so exchanging the leading leg cannot pump the cape. */
  public static float strideClearance(float amplitude, float pivotY, float pivotZ) {
    if (!Float.isFinite(amplitude) || !Float.isFinite(pivotY) || !Float.isFinite(pivotZ)) return 0;
    float peak = 1.4f * Math.max(0, Math.min(1, amplitude));
    // The upper armor corner sweeps a sqrt(10)-pixel circle about the hip. Its
    // tangent also covers the small intermediate maximum between the stride ends.
    double distance = Math.hypot(pivotY, pivotZ - 2);
    if (distance <= Math.sqrt(10)) return 90;
    float hip = (float)Math.toDegrees(Math.atan2(pivotZ - 2, pivotY) + Math.asin(Math.sqrt(10) / distance));
    return Math.max(hip, Math.max(legClearance(peak, pivotY, pivotZ), legClearance(-peak, pivotY, pivotZ))) + .01f;
  }

  /** Minimum angle for the actual sixteen-pixel cape, with armor-expanded legs. */
  public static float legClearance(float pitch, float pivotY, float pivotZ) {
    if (!Float.isFinite(pitch) || !Float.isFinite(pivotY) || !Float.isFinite(pivotZ)) return 0;
    double sine = Math.sin(pitch), cosine = Math.cos(pitch), z = pivotZ - 2;
    double y0 = pivotY - cosine + 3 * sine, z0 = z - sine - 3 * cosine;
    double y1 = pivotY + 13 * cosine + 3 * sine, z1 = z + 13 * sine - 3 * cosine;
    double y2 = pivotY + 13 * cosine - 3 * sine, z2 = z + 13 * sine + 3 * cosine;
    double y3 = pivotY - cosine - 3 * sine, z3 = z - sine + 3 * cosine;
    double angle = Math.max(edgeClearance(y0, z0, y1, z1), edgeClearance(y1, z1, y2, z2));
    angle = Math.max(angle, Math.max(edgeClearance(y2, z2, y3, z3), edgeClearance(y3, z3, y0, z0)));
    return (float)Math.toDegrees(angle);
  }

  private static double edgeClearance(double y, double z, double endY, double endZ) {
    double angle = y * y + z * z <= 256 ? Math.max(0, Math.atan2(z, y)) : 0;
    double dy = endY - y, dz = endZ - z;
    double a = dy * dy + dz * dz, b = 2 * (y * dy + z * dz), c = y * y + z * z - 256;
    double discriminant = b * b - 4 * a * c;
    if (discriminant < 0) return angle;
    double root = Math.sqrt(discriminant);
    for (int sign = -1; sign <= 1; sign += 2) {
      double t = (-b + sign * root) / (2 * a);
      if (t >= 0 && t <= 1) angle = Math.max(angle, Math.atan2(z + t * dz, y + t * dy));
    }
    return angle;
  }
}
