package local.luke.power.inventory;

/** Successful automatic refills may resume the same held placement, never a manual hand swap. */
public final class InventoryRefills {
  @FunctionalInterface public interface Waiting {
    boolean test(Object inventory, HandUse.Hand hand, Object original);
  }
  @FunctionalInterface public interface Replaced {
    void accept(Object inventory, HandUse.Hand hand, Object original, Object replacement);
  }
  private static Waiting waiting = (inventory, hand, original) -> false;
  private static Replaced replaced = (inventory, hand, original, replacement) -> {};
  private InventoryRefills() {}
  public static void waiting(Waiting provider) { waiting = java.util.Objects.requireNonNull(provider); }
  public static void onReplaced(Replaced listener) { replaced = java.util.Objects.requireNonNull(listener); }
  public static boolean waiting(Object inventory, HandUse.Hand hand, Object original) { return waiting.test(inventory, hand, original); }
  public static void replaced(Object inventory, HandUse.Hand hand, Object original, Object replacement) {
    replaced.accept(inventory, hand, original, replacement);
  }
}
