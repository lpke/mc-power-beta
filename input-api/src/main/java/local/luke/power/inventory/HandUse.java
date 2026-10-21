package local.luke.power.inventory;

/** A synchronous native callback scope. Never changes the selected hotbar slot. */
public final class HandUse implements AutoCloseable {
  public enum Hand { MAIN, OFF }
  private static final ThreadLocal<HandUse> CURRENT = new ThreadLocal<>();
  private static long manualRevision;
  private final HandUse previous;
  private final Object inventory;
  private final Hand hand;
  private final boolean selectedSetter;
  private final Thread thread = Thread.currentThread();
  private boolean closed;

  private HandUse(Object inventory, Hand hand, boolean selectedSetter) {
    if (inventory == null || hand == null) throw new IllegalArgumentException("Missing hand owner");
    this.inventory = inventory; this.hand = hand; this.selectedSetter = selectedSetter;
    previous = CURRENT.get(); CURRENT.set(this);
  }
  public static HandUse begin(Object inventory, Hand hand, boolean selectedSetter) {
    return new HandUse(inventory, hand, selectedSetter);
  }
  /** Manual hand swaps end an existing held placement, even if both stacks have identical data. */
  public static void swapping() { manualRevision++; }
  public static long manualRevision() { return manualRevision; }
  public static Hand hand(Object inventory) {
    HandUse value = CURRENT.get();
    return value != null && value.inventory == inventory ? value.hand : Hand.MAIN;
  }
  public static boolean routesSelectedSetter(Object inventory) {
    HandUse value = CURRENT.get();
    return value != null && value.inventory == inventory && value.hand == Hand.OFF && value.selectedSetter;
  }
  @Override public void close() {
    if (closed || thread != Thread.currentThread() || CURRENT.get() != this)
      throw new IllegalStateException("Hand callback scopes must close in order on their owning thread");
    closed = true;
    if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
  }
}
