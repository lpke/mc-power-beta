package local.luke.power.building.hotbar;

import java.util.IdentityHashMap;

/** Plans a permutation without writing to the live inventory or changing any stack. */
public final class InventoryRows {
  private InventoryRows() {}

  public static <T> T[] swapped(T[] inventory, int row) {
    if (inventory == null || inventory.length < 36 || row < 0 || row > 2)
      throw new IllegalArgumentException("Expected a player inventory and row 0 to 2");
    IdentityHashMap<T, Boolean> unique = new IdentityHashMap<>();
    for (T item : inventory)
      if (item != null && unique.put(item, Boolean.TRUE) != null)
        throw new IllegalArgumentException("Two inventory slots reference the same stack");
    T[] result = inventory.clone();
    int start = (row + 1) * 9;
    for (int i = 0; i < 9; i++) {
      result[i] = inventory[start + i];
      result[start + i] = inventory[i];
    }
    return result;
  }

  public static boolean identical(Object[] a, Object[] b) {
    if (a == null || b == null || a.length != b.length) return false;
    for (int i = 0; i < a.length; i++) if (a[i] != b[i]) return false;
    return true;
  }

  public static int scroll(int row, int wheel, boolean reversed) {
    return Math.floorMod(row + Integer.signum(wheel) * (reversed ? 1 : -1), 3);
  }
}
