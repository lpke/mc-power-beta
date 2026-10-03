package local.luke.power.building;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import local.luke.power.building.hotbar.InventoryRows;
import org.junit.jupiter.api.Test;

class InventoryRowsTest {
  @Test
  void swapsEverySlotWithoutChangingOtherRowsOrStackIdentity() {
    for (int row = 0; row < 3; row++) {
      Object[] items = new Object[40];
      for (int i = 0; i < 40; i++) items[i] = i % 7 == 0 ? null : new Object();
      Object[] before = items.clone();
      Object[] source = items;
      items = InventoryRows.swapped(items, row);
      assertArrayEquals(before, source);
      for (int i = 0; i < 9; i++) {
        assertSame(before[(row + 1) * 9 + i], items[i]);
        assertSame(before[i], items[(row + 1) * 9 + i]);
      }
      for (int i = 9; i < 40; i++)
        if (i < (row + 1) * 9 || i >= (row + 2) * 9) assertSame(before[i], items[i]);
      items = InventoryRows.swapped(items, row);
      assertArrayEquals(before, items);
    }
  }

  @Test
  void rejectsInvalidRowsWithoutPartialMutation() {
    Object[] items = new Object[36];
    Arrays.fill(items, new Object());
    Object[] before = items.clone();
    assertThrows(IllegalArgumentException.class, () -> InventoryRows.swapped(items, 3));
    assertArrayEquals(before, items);
    assertThrows(IllegalArgumentException.class, () -> InventoryRows.swapped(new Object[9], 0));
  }

  @Test
  void refusesAliasedStacksBeforeChangingAnything() {
    Object[] inventory = new Object[36];
    inventory[0] = inventory[18] = new Object();
    Object[] before = inventory.clone();
    assertThrows(IllegalArgumentException.class, () -> InventoryRows.swapped(inventory, 1));
    assertArrayEquals(before, inventory);
  }

  @Test
  void scrollWrapsAndReverses() {
    assertEquals(2, InventoryRows.scroll(0, 120, false));
    assertEquals(0, InventoryRows.scroll(2, -120, false));
    assertEquals(1, InventoryRows.scroll(0, 120, true));
  }
}
