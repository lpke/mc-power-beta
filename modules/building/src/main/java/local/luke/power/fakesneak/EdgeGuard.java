package local.luke.power.fakesneak;

/** Native sneak's 0.05-block increments, with the missing Beta diagonal check. */
public final class EdgeGuard {
  @FunctionalInterface
  public interface Support {
    boolean exists(double x, double z);
  }

  public record Motion(double x, double z) {}

  private EdgeGuard() {}

  private static double reduce(double v) {
    return Math.abs(v) <= 0.05 ? 0 : v - Math.copySign(0.05, v);
  }

  public static Motion clip(double x, double z, Support support) {
    // Bound work for corrupt or unusually large move calls. Teleports do not use this path.
    if (!Double.isFinite(x) || !Double.isFinite(z) || Math.abs(x) > 16 || Math.abs(z) > 16)
      return new Motion(0, 0);
    while (x != 0 && !support.exists(x, 0)) x = reduce(x);
    while (z != 0 && !support.exists(0, z)) z = reduce(z);
    while (x != 0 && z != 0 && !support.exists(x, z)) {
      x = reduce(x);
      z = reduce(z);
    }
    return new Motion(x, z);
  }
}
