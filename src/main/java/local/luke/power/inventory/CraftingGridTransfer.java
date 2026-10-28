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
  private record Queued(Container container, int source, PlayerEntity player, Object screen, ItemStack reference, String data) {}
  private static final java.util.LinkedHashMap<Integer, Queued> queued = new java.util.LinkedHashMap<>();
  private static long queuedRevision;
  public static void tick(Minecraft mc) {
    if (queued.isEmpty()) return;
    if (!VisualConfig.current().shiftClickIntoPlayerCraftingGrid) { queued.clear(); return; }
    Queued first = queued.values().iterator().next();
    if (InventoryActions.revision() != queuedRevision || mc.player != first.player || mc.currentScreen != first.screen || mc.world != first.player.world
        || !org.lwjgl.opengl.Display.isActive() || NativeInventoryPlan.failed()
        || mc.player.container != first.container || mc.player.inventory.getCursorStack() != null
        || NativeInventoryPlan.busy() && !NativeInventoryPlan.ownsAction(mc, "crafting_grid_transfer")) { queued.clear(); return; }
    if (NativeInventoryPlan.busy()) return;
    if (!currentGesture(mc, first.container)) return;
    queued.remove(first.source);
    try {
      ItemStack source = first.container.method_2084(first.source).getStack();
      if (source == first.reference && local.luke.power.death.DeathItems.encode(source).equals(first.data))
        playerGridFallback(first.container, first.source, first.player);
      queuedRevision = InventoryActions.revision();
    } catch (java.io.IOException | RuntimeException changed) { queued.clear(); }
  }
  private CraftingGridTransfer() {}
  /** Run before a queued plan snapshots its baseline, and once before publishing owned writes. */
  static boolean currentGesture(Minecraft mc, Object owner) {
    if (mc.currentScreen instanceof local.luke.power.input.InventoryDrag drag && drag.power$itemDragCurrent(owner)) return true;
    queued.clear();
    if (mc.currentScreen instanceof local.luke.power.input.InventoryDrag drag) drag.power$cancelItemDrag();
    return false;
  }

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
      if (existing == null || stack.isStackable() && sameIdentity(stack, existing)
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
  private static boolean sameIdentity(ItemStack first, ItemStack second) {
    if (first == null || second == null || first.itemId != second.itemId || first.getDamage() != second.getDamage()) return false;
    try {
      ItemStack a = first.clone(), b = second.clone(); a.count = b.count = 1;
      return local.luke.power.death.DeathItems.encode(a).equals(local.luke.power.death.DeathItems.encode(b));
    } catch (java.io.IOException unavailable) { return false; }
  }
  private static boolean playerGridFallback(Container container, int sourceId, PlayerEntity player) {
    if (!VisualConfig.current().shiftClickIntoPlayerCraftingGrid || sourceId < 9 || sourceId >= 45 || player.world == null || player.world.isRemote
        || player.container != container || player.inventory.getCursorStack() != null) return false;
    var mc = (Minecraft)net.fabricmc.loader.api.FabricLoader.getInstance().getGameInstance();
    if (mc == null || mc.player != player || !(mc.currentScreen instanceof net.minecraft.client.gui.screen.container.ContainerScreen screen)) return false;
    var context = local.luke.power.recipes.RecipeContext.of(screen);
    if (context == null || context.width != 2 || context.handler != container || !context.writable(mc)) return false;
    Slot source = container.method_2084(sourceId); ItemStack stack = source.getStack();
    var access = (local.luke.power.mixin.DeathSlotAccessor)source;
    if (access.power$deathInventory() != player.inventory || access.power$deathIndex() < 0
        || access.power$deathIndex() >= 36 || stack == null || stack.count <= 0 || stack.count > stack.getMaxCount()) return false;
    // Give the normal storage/hotbar destination priority whenever it has any capacity.
    int targetStart = sourceId < 36 ? 36 : 9, targetEnd = sourceId < 36 ? 45 : 36;
    for (int id = targetStart; id < targetEnd; id++) {
      Slot slot = container.method_2084(id); ItemStack existing = slot.getStack();
      if (slot.canInsert(stack) && (existing == null || stack.isStackable() && sameIdentity(stack, existing)
          && existing.count < Math.min(existing.getMaxCount(), slot.getMaxItemCount()))) return false;
    }
    try {
      if (NativeInventoryPlan.busy()) {
        if (NativeInventoryPlan.ownsAction(mc, "crafting_grid_transfer") && queued.size() < 36) {
          queuedRevision = InventoryActions.revision();
          queued.putIfAbsent(sourceId, new Queued(container, sourceId, player, mc.currentScreen, stack,
              local.luke.power.death.DeathItems.encode(stack)));
        }
        return true;
      }
      var output = new local.luke.power.recipes.RecipeOutputGuard(mc, context);
      List<NativeInventoryPlan.Change> changes = new ArrayList<>(); int remaining = stack.count;
      for (boolean empty : new boolean[] {false, true}) for (Slot slot : context.inputs) {
        ItemStack existing = slot.getStack();
        if (remaining == 0 || (existing == null) != empty || !slot.canInsert(stack)
            || existing != null && (!stack.isStackable() || !sameIdentity(stack, existing))) continue;
        int count = existing == null ? 0 : existing.count;
        int amount = Math.min(remaining, Math.max(0, Math.min(stack.getMaxCount(), slot.getMaxItemCount()) - count));
        if (amount == 0) continue;
        ItemStack next = ItemStack.clone(existing == null ? stack : existing); next.count = count + amount;
        changes.add(output.input(slot, next)); remaining -= amount;
      }
      if (changes.isEmpty()) return false;
      ItemStack left = remaining == 0 ? null : ItemStack.clone(stack); if (left != null) left.count = remaining;
      changes.add(0, NativeInventoryPlan.Change.main(player.inventory, access.power$deathIndex(), left));
      NativeInventoryPlan.submit(mc, "crafting_grid_transfer", true, changes,
          () -> VisualConfig.current().shiftClickIntoPlayerCraftingGrid && context.writable(mc) && output.matches(),
          () -> {
            if (!output.matches()) throw new IllegalStateException("Crafting output changed during transfer");
            if (mc.currentScreen instanceof local.luke.power.input.InventoryDrag drag) {
              var owned = new ArrayList<Slot>(context.inputs); owned.add(source);
              drag.power$acceptItemDragChanges(container, owned);
            }
          }, output::restore);
      // A pending/rejected protected operation must not fall through to a second native gesture.
      return true;
    } catch (java.io.IOException failure) {
      PowerBeta.LOG.warn("Crafting storage transfer declined", failure); return true;
    }
  }
}
