package local.luke.power.input;

/** A text-bearing inventory suspends manual movement and background inventory shortcuts. */
public final class InventoryTextInput {
  private static Object owner;
  private InventoryTextInput() {}
  public static void focus(Object screen, boolean focused) {
    if (focused) owner = java.util.Objects.requireNonNull(screen);
    else if (owner == screen) owner = null;
  }
  public static boolean active(Object screen) { return screen != null && screen == owner; }
  public static void clear() { owner = null; }
}
