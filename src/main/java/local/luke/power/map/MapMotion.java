package local.luke.power.map;

/** Render interpolation is independent of the tick-owned terrain update budget. */
public final class MapMotion {
  private MapMotion() {}

  public static double position(double previous, double current, float delta) {
    if (!Double.isFinite(previous) || Math.abs(current - previous) > 64) return current;
    return previous + (current - previous) * Math.max(0, Math.min(1, delta));
  }

  public static float angle(float previous, float current, float delta) {
    float distance = (current - previous) % 360;
    if (distance >= 180) distance -= 360;
    if (distance < -180) distance += 360;
    return previous + distance * Math.max(0, Math.min(1, delta));
  }
}
