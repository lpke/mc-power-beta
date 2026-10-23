package local.luke.power.input;

/** A text-bearing inventory suspends manual movement and background inventory shortcuts. */
public final class InventoryTextInput {
  private static Object owner;
  private static final java.util.Set<Object> claims = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
  private InventoryTextInput() {}
  public static void focus(Object screen, boolean focused) {
    focus(screen, screen, focused);
  }
  /** One overlay may release only its own claim, including when disabled or rebuilt. */
  public static void focus(Object screen, Object claim, boolean focused) {
    if (focused) {
      java.util.Objects.requireNonNull(screen); java.util.Objects.requireNonNull(claim);
      if (owner != screen) { claims.clear(); owner = screen; }
      claims.add(claim);
    } else if (owner == screen) {
      claims.remove(claim);
      if (claims.isEmpty()) owner = null;
    }
  }
  public static boolean active(Object screen) { return screen != null && screen == owner && !claims.isEmpty(); }
  public static void clear() { owner = null; claims.clear(); }
}
