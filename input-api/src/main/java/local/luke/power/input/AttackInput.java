package local.luke.power.input;

import java.util.function.BooleanSupplier;

/** Supplies a held attack action without synthesizing OS input. */
public final class AttackInput {
  private static BooleanSupplier held = () -> false;
  private AttackInput() {}
  public static void register(BooleanSupplier provider) { held = java.util.Objects.requireNonNull(provider); }
  public static boolean held() { return held.getAsBoolean(); }
}
