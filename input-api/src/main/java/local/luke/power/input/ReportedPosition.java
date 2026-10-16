package local.luke.power.input;

import java.util.function.Supplier;

/** Coordinate reporting shared by modules without referring to a game mapping. */
public final class ReportedPosition {
  public record Pose(double x, double y, double z, float yaw, float pitch) {
    public boolean finite() {
      return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
          && Float.isFinite(yaw) && Float.isFinite(pitch);
    }
  }
  private static Supplier<?> owner = () -> null;
  private static Supplier<Pose> source = () -> null;
  private ReportedPosition() {}
  public static void register(Supplier<?> player, Supplier<Pose> provider) {
    owner = java.util.Objects.requireNonNull(player);
    source = java.util.Objects.requireNonNull(provider);
  }
  public static Pose current(Object player, double x, double y, double z, float yaw, float pitch) {
    Pose pose = player != null && player == owner.get() ? source.get() : null;
    return pose != null && pose.finite() ? pose : new Pose(x, y, z, yaw, pitch);
  }
}
