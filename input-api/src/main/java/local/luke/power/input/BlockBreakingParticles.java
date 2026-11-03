package local.luke.power.input;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** A visit-local inversion of the saved particle preference. */
public final class BlockBreakingParticles {
  private static BooleanSupplier preference = () -> true;
  private static BooleanSupplier disableAll = () -> false;
  private static boolean inverted;
  private BlockBreakingParticles() {}

  public static void configure(BooleanSupplier savedPreference, BooleanSupplier allDisabled) {
    preference = Objects.requireNonNull(savedPreference);
    disableAll = Objects.requireNonNull(allDisabled);
  }
  public static boolean enabled() { return enabled(preference.getAsBoolean(), disableAll.getAsBoolean()); }
  public static boolean enabled(boolean savedPreference, boolean allDisabled) {
    return !allDisabled && savedPreference != inverted;
  }
  public static boolean inverted() { return inverted; }
  public static void toggle() { inverted = !inverted; }
  public static void reset() { inverted = false; }
}
