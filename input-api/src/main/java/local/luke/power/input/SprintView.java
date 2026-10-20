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
  private static final boolean[] active = new boolean[Kind.values().length];
  private static DoubleUnaryOperator fov = delta -> 1;
  private static Object landingPlayer, landingWorld;

  private SprintView() {}

  public static void publish(Kind kind, boolean active, double ratio) {
    SprintView.active[kind.ordinal()] = active;
    ratios[kind.ordinal()] = active && Double.isFinite(ratio) ? Math.max(1, ratio) : 1;
  }

  /** Activity remains explicit when a sprint preference uses normal movement speed. */
  public static boolean active(Kind kind) {
    return active[kind.ordinal()];
  }

  public static double ratio(Kind kind) {
    return ratios[kind.ordinal()];
  }

  /** One game-thread handoff between movement modules, scoped to the same player and world. */
  public static void landed(Object player, Object world) {
    landingPlayer = player;
    landingWorld = world;
  }

  public static boolean consumeLanding(Object player, Object world) {
    boolean inherit = player != null && world != null && landingPlayer == player && landingWorld == world;
    clearLanding();
    return inherit;
  }

  public static void clearLanding() { landingPlayer = landingWorld = null; }

  public static void configure(DoubleUnaryOperator provider) {
    fov = java.util.Objects.requireNonNull(provider);
  }

  public static float fov(float delta) {
    return (float) fov.applyAsDouble(Math.max(0, Math.min(1, delta)));
  }
}
