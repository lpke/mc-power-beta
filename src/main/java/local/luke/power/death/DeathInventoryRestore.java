package local.luke.power.death;

import java.io.IOException;
import java.util.Objects;
import local.luke.power.offhand.Offhand;
import net.minecraft.class_277;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

/** Synchronous death/load restoration. A failed callback never grants ownership of its changes. */
final class DeathInventoryRestore {
  @FunctionalInterface interface Admission { boolean allowed() throws IOException; }
  private final PlayerEntity player;
  private final Object world;
  private final PlayerInventory inventory;
  private final ItemStack[] main, armor, grid, before, expected;
  private final String[] beforeBytes, expectedBytes;
  private final class_277 container;
  private final net.minecraft.class_159 crafting;
  private final ItemStack output;
  private final String outputBytes;
  private final int selected;
  private final boolean[] written = new boolean[46];
  private final Admission admitted;
  private int expectedSelection;
  private boolean selectionWritten;

  DeathInventoryRestore(PlayerEntity player, Admission admitted) throws IOException {
    this.player = player; this.admitted = admitted; world = player.world;
    inventory = player.inventory; main = inventory.main; armor = inventory.armor;
    if (!(player.playerContainer instanceof class_277 nativeContainer)) throw new IOException("Player crafting owner unavailable");
    container = nativeContainer; crafting = container.field_1124;
    grid = ((local.luke.power.mixin.DeathCraftingAccessor)crafting).power$deathStacks();
    if (grid.length != 4) throw new IOException("Unsupported player crafting layout");
    before = DeathItems.snapshot(inventory, container); expected = before.clone();
    beforeBytes = bytes(before); expectedBytes = beforeBytes.clone();
    var unique = new java.util.IdentityHashMap<ItemStack, Boolean>();
    for (ItemStack item : before)
      if (item != null && unique.put(item, true) != null) throw new IOException("Shared death restoration stack");
    selected = expectedSelection = inventory.selectedSlot;
    output = container.method_2084(0).getStack(); outputBytes = bytes(output);
  }

  void publish(ItemStack[] next, int selection) throws IOException {
    if (next.length != 46 || selection < 0 || selection > 8) throw new IOException("Invalid restoration layout");
    String[] nextBytes = bytes(next);
    requireExpected();
    selectionWritten = true; expectedSelection = selection; inventory.selectedSlot = selection;
    for (int slot = 0; slot < 46; slot++) {
      // Restore the offhand before crafting callbacks, as part of the protected inventory.
      int index = slot < 41 ? slot : slot == 41 ? 45 : slot - 1;
      requireExpected();
      written[index] = true; expected[index] = next[index]; expectedBytes[index] = nextBytes[index];
      if (index < 40) inventory.setStack(index, next[index]);
      else if (index == 40) inventory.setCursorStack(next[index]);
      else if (index == 45) Offhand.set(inventory, next[index]);
      else container.method_2084(index - 40).setStack(next[index]);
      requireExpected();
    }
    inventory.markDirty(); requireExpected();
  }

  private boolean owns() {
    return player.world == world && player.inventory == inventory && inventory.main == main && inventory.armor == armor
        && player.playerContainer == container && container.field_1124 == crafting
        && ((local.luke.power.mixin.DeathCraftingAccessor)crafting).power$deathStacks() == grid;
  }

  private void requireExpected() throws IOException {
    if (!owns() || !admitted.allowed() || inventory.selectedSlot != expectedSelection)
      throw new IOException("Death restoration owner changed");
    ItemStack[] actual = DeathItems.snapshot(inventory, container);
    for (int i = 0; i < actual.length; i++)
      if (actual[i] != expected[i] || !Objects.equals(bytes(actual[i]), expectedBytes[i]))
        throw new IOException("Native callback changed a protected death restoration slot");
  }

  /** Returns true only when the complete original inventory and derived output were restored. */
  boolean rollback(Throwable failure) {
    if (!owns()) return false;
    for (int i = 45; i >= 0; i--) {
      if (!written[i] || !owns()) continue;
      try {
        ItemStack actual = DeathItems.snapshot(inventory, container)[i];
        if (actual != expected[i] || !Objects.equals(bytes(actual), expectedBytes[i])) continue;
        ItemStack original = original(before[i], beforeBytes[i]);
        if (i < 36) main[i] = original;
        else if (i < 40) armor[i - 36] = original;
        else if (i == 40) inventory.setCursorStack(original);
        else if (i == 45) Offhand.set(inventory, original);
        else grid[i - 41] = original;
      } catch (Exception | LinkageError rollback) { failure.addSuppressed(rollback); }
    }
    try {
      if (owns() && selectionWritten && inventory.selectedSlot == expectedSelection) inventory.selectedSlot = selected;
      if (!restored()) return false;
      container.method_2084(0).setStack(original(output, outputBytes));
      inventory.markDirty();
      return restored() && Objects.equals(bytes(container.method_2084(0).getStack()), outputBytes);
    } catch (Exception | LinkageError rollback) { failure.addSuppressed(rollback); return false; }
  }

  private boolean restored() throws IOException {
    if (!owns() || !admitted.allowed() || inventory.selectedSlot != selected) return false;
    return java.util.Arrays.equals(bytes(DeathItems.snapshot(inventory, container)), beforeBytes);
  }
  private static ItemStack original(ItemStack reference, String encoded) throws IOException {
    try { if (Objects.equals(bytes(reference), encoded)) return reference; }
    catch (IOException changed) { /* A callback may also have invalidated the old reference. */ }
    return encoded == null ? null : DeathItems.decode(encoded);
  }
  private static String bytes(ItemStack item) throws IOException { return item == null ? null : DeathItems.encode(item); }
  private static String[] bytes(ItemStack[] items) throws IOException {
    String[] result = new String[items.length];
    for (int i = 0; i < items.length; i++) result[i] = bytes(items[i]);
    return result;
  }
}
