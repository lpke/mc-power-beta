package local.luke.power.inventory;

import java.util.ArrayList;
import java.util.List;
import local.luke.power.PowerBeta;
import local.luke.power.visual.VisualConfig;
import net.minecraft.class_196;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/** Uses normal slot clicks, including StationAPI's NBT-aware splitting and merging. */
public final class CraftingGridTransfer {
  private static boolean failed;
  private CraftingGridTransfer() {}

  public static boolean transfer(Container container, int sourceId, PlayerEntity player) {
    if (failed || !VisualConfig.current().shiftClickIntoCraftingGrid
        || player.world == null || player.world.isRemote || player.dead || player.health <= 0
        || player.container != container || player.inventory.getCursorStack() != null) return false;
    if (container.getClass() != class_196.class || sourceId < 10 || sourceId >= 46
        || container.slots.size() != 46 || !container.method_2094(player)) return false;
    Slot source = container.method_2084(sourceId);
    ItemStack stack = source.getStack();
    if (stack == null || stack.count <= 0 || stack.count > stack.getMaxCount()) return false;

    List<Slot> targets = new ArrayList<>();
    // Match existing ingredients first, preserving the recipe's occupied positions.
    for (boolean empty : new boolean[] {false, true}) for (int i = 1; i <= 9; i++) {
      Slot slot = container.method_2084(i);
      ItemStack existing = slot.getStack();
      if ((existing == null) != empty || !slot.canInsert(stack) || slot.getMaxItemCount() <= 0) continue;
      if (existing == null || stack.isStackable() && stack.isItemEqual(existing)
          && existing.count < Math.min(existing.getMaxCount(), slot.getMaxItemCount())) targets.add(slot);
    }
    if (targets.isEmpty()) return false;

    // Prepare complete copies before the first click. A failure restores inputs and
    // source in this same game tick; restoring inputs recalculates the recipe output.
    List<Slot> changed = new ArrayList<>(targets); changed.add(source);
    List<ItemStack> before = changed.stream().map(slot -> ItemStack.clone(slot.getStack())).toList();
    try {
      container.onSlotClick(sourceId, 0, false, player);
      for (Slot slot : targets) {
        if (player.inventory.getCursorStack() == null) break;
        container.onSlotClick(slot.id, 0, false, player);
      }
      if (player.inventory.getCursorStack() != null) container.onSlotClick(sourceId, 0, false, player);
      if (player.inventory.getCursorStack() != null) throw new IllegalStateException("Crafting transfer left a cursor stack");
    } catch (RuntimeException error) {
      failed = true;
      // Never retry a failed operation or let the original Shift-click run afterwards.
      for (int i = 0; i < changed.size(); i++) changed.get(i).setStack(before.get(i));
      player.inventory.setCursorStack(null);
      PowerBeta.LOG.error("Crafting grid transfer restored; disabled until restart", error);
    }
    return true;
  }
}
