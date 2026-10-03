package local.luke.tweaks.hotbar;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Single-reference commit; all allocation, serialization and disk writes precede publication. */
public final class InventoryExchange {
  private InventoryExchange() {}

  @FunctionalInterface
  public interface Checked {
    void run() throws Exception;
  }

  public static <T> void commit(
      Supplier<T[]> current,
      Consumer<T[]> publish,
      T[] original,
      T[] expected,
      T[] next,
      T[] rollback,
      Checked prepare,
      Checked verify)
      throws Exception {
    if (original == null
        || expected == null
        || next == null
        || rollback == null
        || expected.length != original.length
        || next.length != original.length
        || rollback.length != original.length
        || original == next
        || next == rollback
        || !InventoryRows.identical(original, expected))
      throw new IllegalArgumentException("Invalid inventory transaction");
    T[] planned = next.clone();
    prepare.run();
    if (current.get() != original || !InventoryRows.identical(original, expected))
      throw new IllegalStateException("Inventory changed while preparing the swap");
    try {
      publish.accept(next);
      verify.run();
      if (current.get() != next || !InventoryRows.identical(next, planned))
        throw new IllegalStateException("Inventory changed during the swap");
    } catch (Throwable failure) {
      // Never overwrite a replacement inventory installed by unrelated code.
      if (current.get() == next) publish.accept(rollback);
      if (failure instanceof Error error) throw error;
      throw (Exception) failure;
    }
  }
}
