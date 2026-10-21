package local.luke.power.input;

/** Queued wheel ownership shared by inventory modules with different Minecraft mappings. */
public final class InventoryWheel {
  private static Object owner;
  private static long pending;
  private static int current;
  private InventoryWheel() {}

  public static void context(Object screen, boolean focused) {
    if (!focused || owner != screen) { pending = 0; current = 0; }
    owner = focused ? screen : null;
  }

  /** Called once for the native event, after the minimap has had its ownership check. */
  public static void event(Object screen, boolean focused, int amount, boolean claimed) {
    context(screen, focused);
    current = owner == null || claimed ? 0 : amount;
    pending += current;
  }

  /** Slot transfers may consume only the event whose pointer is currently over their slot. */
  public static int takeEvent(Object screen, boolean focused) {
    context(screen, focused);
    int amount = current;
    current = 0; pending -= amount;
    return amount;
  }

  /** Creative consumes remaining events after dispatch, while rendering its catalog. */
  public static int take(Object screen, boolean focused) {
    context(screen, focused);
    int amount = (int)Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, pending));
    pending -= amount; current = 0;
    return amount;
  }

  public static void finishEvent(Object screen, boolean catalog) {
    if (owner != screen) return;
    if (!catalog) pending -= current;
    current = 0;
  }

  public static void clear() { owner = null; pending = 0; current = 0; }
}
