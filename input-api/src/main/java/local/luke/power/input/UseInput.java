package local.luke.power.input;

import java.util.function.BooleanSupplier;

/** Supplies native held-use repeats without synthesizing pointer events. */
public final class UseInput {
  private static BooleanSupplier held = () -> false;
  private static boolean pending;
  private UseInput() {}
  public static void register(BooleanSupplier provider) { held = java.util.Objects.requireNonNull(provider); }
  public static boolean held() { return held.getAsBoolean(); }
  public static void press() { pending = true; }
  public static void cancelPress() { pending = false; }
  public static boolean consumePress() {
    boolean use = pending && held(); pending = false; return use;
  }
}
