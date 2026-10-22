package local.luke.power.inventory;

import java.util.ArrayList;
import java.util.List;
import local.luke.power.PowerBeta;
import local.luke.power.visual.VisualConfig;
import net.minecraft.class_196;
import net.minecraft.class_277;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/** Uses normal slot clicks, including StationAPI's NBT-aware splitting and merging. */
public final class CraftingGridTransfer {
  private static boolean failed;
  private CraftingGridTransfer() {}

  public static boolean transfer(Container container, int sourceId, PlayerEntity player) {
    if (container.getClass() == class_277.class) return playerGridFallback(container, sourceId, player);
    if (failed || !VisualConfig.current().shiftClickIntoCraftingGrid
        || player.world == null || player.world.isRemote || player.dead || player.health <= 0
        || player.container != container || player.inventory.getCursorStack() != null) return false;
    if (container.getClass() != class_196.class || sourceId < 10 || sourceId >= 46
        || container.slots.size() != 46 + (local.luke.power.offhand.OffhandContainers.slot(container, player.inventory) == null ? 0 : 1)
        || !container.method_2094(player)) return false;
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
  private static boolean playerGridFallback(Container container, int sourceId, PlayerEntity player) {
    if (!storedPlayerGrid() || sourceId < 9 || sourceId >= 36 || player.world == null || player.world.isRemote
        || player.container != container || player.inventory.getCursorStack() != null) return false;
    var mc = (Minecraft)net.fabricmc.loader.api.FabricLoader.getInstance().getGameInstance();
    if (mc == null || mc.player != player || !(mc.currentScreen instanceof net.minecraft.client.gui.screen.container.ContainerScreen screen)) return false;
    var context = local.luke.power.recipes.RecipeContext.of(screen);
    if (context == null || context.width != 2 || context.handler != container || !context.writable(mc)) return false;
    Slot source = container.method_2084(sourceId); ItemStack stack = source.getStack();
    var access = (local.luke.power.mixin.DeathSlotAccessor)source;
    if (access.power$deathInventory() != player.inventory || access.power$deathIndex() < 9
        || access.power$deathIndex() >= 36 || stack == null || stack.count <= 0 || stack.count > stack.getMaxCount()) return false;
    // Let native Shift-click own every transfer that can move anything into the hotbar.
    for (int id = 36; id < 45; id++) {
      Slot slot = container.method_2084(id); ItemStack existing = slot.getStack();
      if (slot.canInsert(stack) && (existing == null || stack.isStackable() && stack.isItemEqual(existing)
          && existing.count < Math.min(existing.getMaxCount(), slot.getMaxItemCount()))) return false;
    }
    try {
      var output = new local.luke.power.recipes.RecipeOutputGuard(mc, context);
      List<NativeInventoryPlan.Change> changes = new ArrayList<>(); int remaining = stack.count;
      for (boolean empty : new boolean[] {false, true}) for (Slot slot : context.inputs) {
        ItemStack existing = slot.getStack();
        if (remaining == 0 || (existing == null) != empty || !slot.canInsert(stack)
            || existing != null && (!stack.isStackable() || !stack.isItemEqual(existing))) continue;
        int count = existing == null ? 0 : existing.count;
        int amount = Math.min(remaining, Math.max(0, Math.min(stack.getMaxCount(), slot.getMaxItemCount()) - count));
        if (amount == 0) continue;
        ItemStack next = ItemStack.clone(existing == null ? stack : existing); next.count = count + amount;
        changes.add(output.input(slot, next)); remaining -= amount;
      }
      if (changes.isEmpty()) return false;
      ItemStack left = remaining == 0 ? null : ItemStack.clone(stack); if (left != null) left.count = remaining;
      changes.add(0, NativeInventoryPlan.Change.main(player.inventory, access.power$deathIndex(), left));
      NativeInventoryPlan.submit(mc, "crafting_storage_transfer", true, changes,
          () -> storedPlayerGrid() && context.writable(mc) && output.matches(),
          () -> { if (!output.matches()) throw new IllegalStateException("Crafting output changed during transfer"); }, output::restore);
      // A pending/rejected protected operation must not fall through to a second native gesture.
      return true;
    } catch (java.io.IOException failure) {
      PowerBeta.LOG.warn("Crafting storage transfer declined", failure); return true;
    }
  }
  private static boolean storedPlayerGrid() {
    try {
      Object config = Class.forName("local.luke.power.compat.Config").getField("config").get(null);
      return Boolean.TRUE.equals(config.getClass().getField("allowCraftingInventorySlots").get(config));
    } catch (ReflectiveOperationException unavailable) { return false; }
  }
}
