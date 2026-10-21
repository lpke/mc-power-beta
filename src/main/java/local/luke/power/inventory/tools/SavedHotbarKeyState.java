package local.luke.power.inventory.tools;

import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;

/** One decision per native event, retained through repeated reads and either release order. */
public final class SavedHotbarKeyState {
  private long timestamp;
  private int key, held;
  private boolean seen, down, repeat, consumed;

  public boolean event(long timestamp, int key, boolean down, boolean repeat, BooleanSupplier action) {
    if (key < 2 || key > 10) return false;
    if (seen && this.timestamp == timestamp && this.key == key && this.down == down && this.repeat == repeat) return consumed;
    seen = true; this.timestamp = timestamp; this.key = key; this.down = down; this.repeat = repeat; consumed = false;
    int bit = 1 << (key - 2);
    if (!down) { held &= ~bit; return false; }
    if ((held & bit) != 0) return consumed = true;
    if (!repeat && action.getAsBoolean()) { held |= bit; consumed = true; }
    return consumed;
  }

  /** Menus can drain releases themselves. Poll only the few number presses this owner consumed. */
  public void reconcile(IntPredicate physical) {
    for (int i = 0; i < 9; i++) if ((held & 1 << i) != 0 && !physical.test(i + 2)) held &= ~(1 << i);
  }
}
