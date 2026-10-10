package local.luke.power.input;

import java.util.function.BooleanSupplier;

/** Live camera state, independent of movement ownership and module mappings. */
public final class DetachedCamera {
  private static BooleanSupplier active = () -> false;
  private DetachedCamera() {}

  public static void register(BooleanSupplier value) { active = value; }
  public static boolean isActive() { return active.getAsBoolean(); }
}
