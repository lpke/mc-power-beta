package local.luke.power.creative.inventory;

/** Native inventory model rendering never changes the player's world visibility or mode. */
public final class InventoryPreview {
  private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);
  private InventoryPreview() {}
  public static boolean active() { return ACTIVE.get(); }
  public static void render(Runnable draw) {
    boolean previous = ACTIVE.get();
    ACTIVE.set(true);
    try { draw.run(); }
    finally { if (previous) ACTIVE.set(true); else ACTIVE.remove(); }
  }
}
