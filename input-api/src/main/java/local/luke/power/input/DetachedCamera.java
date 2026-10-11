package local.luke.power.input;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

/** Live camera state, independent of movement ownership and module mappings. */
public final class DetachedCamera {
  private static BooleanSupplier active = () -> false;
  private static DoubleSupplier x = () -> 0, y = () -> 0, z = () -> 0;
  private DetachedCamera() {}

  public static void register(BooleanSupplier value) { active = value; }
  public static void register(BooleanSupplier value, DoubleSupplier cameraX,
      DoubleSupplier cameraY, DoubleSupplier cameraZ) {
    active = value;
    x = cameraX; y = cameraY; z = cameraZ;
  }
  public static boolean isActive() { return active.getAsBoolean(); }
  public static double x() { return x.getAsDouble(); }
  public static double y() { return y.getAsDouble(); }
  public static double z() { return z.getAsDouble(); }
}
