package local.luke.power.input;

import java.util.function.DoubleUnaryOperator;
import java.util.function.BooleanSupplier;

/** The renderer reads tick-owned bow state without polling use keys. */
public final class BowView {
  private static DoubleUnaryOperator fov = delta -> 1;
  private static BooleanSupplier active = () -> false;
  private BowView() {}
  public static void register(BooleanSupplier enabled, DoubleUnaryOperator provider) {
    active = java.util.Objects.requireNonNull(enabled);fov = java.util.Objects.requireNonNull(provider);
  }
  public static float apply(float original,float delta) {
    return active.getAsBoolean() ? original * (float)fov.applyAsDouble(delta) : original;
  }
}
