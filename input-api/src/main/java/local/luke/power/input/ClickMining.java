package local.luke.power.input;

/** A visit-local inversion of the saved preference; never writes configuration. */
public final class ClickMining {
  private static boolean inverted;
  private ClickMining() {}
  public static boolean inverted() { return inverted; }
  public static boolean enabled(boolean preference) { return preference != inverted; }
  public static void toggle() { inverted = !inverted; }
  public static void reset() { inverted = false; }
}
