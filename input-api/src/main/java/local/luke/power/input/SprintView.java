package local.luke.power.input;

import java.util.function.DoubleUnaryOperator;

/**
 * Published movement state. Rendering never polls sprint bindings or infers activity from settings.
 */
public final class SprintView {
  public enum Kind {
    GROUND,
    CREATIVE,
    SPECTATOR,
    FREECAM
  }

  private static final double[] ratios = {1, 1, 1, 1};
  private static DoubleUnaryOperator fov = delta -> 1;

  private SprintView() {}

  public static void publish(Kind kind, boolean active, double ratio) {
    ratios[kind.ordinal()] = active && Double.isFinite(ratio) ? Math.max(1, ratio) : 1;
  }

  public static double ratio(Kind kind) {
    return ratios[kind.ordinal()];
  }

  public static void configure(DoubleUnaryOperator provider) {
    fov = java.util.Objects.requireNonNull(provider);
  }

  public static float fov(float delta) {
    return (float) fov.applyAsDouble(Math.max(0, Math.min(1, delta)));
  }
}
