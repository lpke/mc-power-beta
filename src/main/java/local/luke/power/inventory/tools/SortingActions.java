package local.luke.power.inventory.tools;

import java.io.IOException;
import java.util.*;
import local.luke.power.death.DeathItems;
import local.luke.power.inventory.NativeInventoryPlan;
import local.luke.power.mixin.DeathSlotAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public final class SortingActions {
  private SortingActions() {}
  public static boolean sort(Minecraft mc) { return sort(mc, false); }
  public static boolean sort(Minecraft mc, boolean playerStorage) {
    if (!InventoryToolsConfig.current().sorting || !SortingSlots.eligible(mc)) return false;
    try {
      var regions = SortingSlots.read(mc); List<Slot> slots = playerStorage || regions.storage().isEmpty() ? regions.player() : regions.storage();
      var before = capture(slots); var after = SortingPlanner.sort(before);
      List<NativeInventoryPlan.Change> changes = changes(mc, slots, before, after);
      return NativeInventoryPlan.submit(mc, "inventory_sort", true, changes,
          () -> InventoryToolsConfig.current().sorting && SortingSlots.owns(mc, regions), () -> {});
    } catch (IOException | RuntimeException failure) { feedback(mc, failure.getMessage()); return false; }
  }
  public static boolean depositMatching(Minecraft mc) {
    if (!InventoryToolsConfig.current().depositMatching || !SortingSlots.eligible(mc)) return false;
    try {
      var regions = SortingSlots.read(mc); if (regions.storage().isEmpty()) return false;
      var source = capture(regions.player()); var destination = capture(regions.storage()); var planned = SortingPlanner.deposit(source, destination);
      List<NativeInventoryPlan.Change> changes = new ArrayList<>(changes(mc, regions.player(), source, planned.source()));
      changes.addAll(changes(mc, regions.storage(), destination, planned.destination()));
      return NativeInventoryPlan.submit(mc, "deposit_matching", true, changes,
          () -> InventoryToolsConfig.current().depositMatching && SortingSlots.owns(mc, regions), () -> {});
    } catch (IOException | RuntimeException failure) { feedback(mc, failure.getMessage()); return false; }
  }
  private static List<SortingPlanner.Cell> capture(List<Slot> slots) throws IOException {
    List<SortingPlanner.Cell> cells = new ArrayList<>();
    for (Slot slot : slots) {
      ItemStack stack = slot.getStack(); int limit = slot.getMaxItemCount();
      if (limit <= 0) throw new IOException("This native slot has no writable capacity.");
      SortingPlanner.Item item = null;
      if (stack != null) {
        String data = DeathItems.encode(stack); DeathItems.decode(data);
        ItemStack identity = DeathItems.decode(data); identity.count = 1;
        item = new SortingPlanner.Item(DeathItems.encode(identity), data, stack.itemId, stack.getDamage(), stack.count, stack.getMaxCount(), stack.isStackable());
      }
      boolean pinned = stack != null && (stack.count > Math.min(limit, stack.getMaxCount()) || !slot.canInsert(stack));
      cells.add(new SortingPlanner.Cell(item, limit, pinned));
    }
    return List.copyOf(cells);
  }
  private static List<NativeInventoryPlan.Change> changes(Minecraft mc, List<Slot> slots, List<SortingPlanner.Cell> before, List<SortingPlanner.Cell> after) throws IOException {
    List<NativeInventoryPlan.Change> changes = new ArrayList<>();
    for (int i = 0; i < slots.size(); i++) {
      var item = after.get(i).item(); ItemStack stack = item == null ? null : DeathItems.decode(item.data());
      if (stack != null) stack.count = item.count();
      String next = stack == null ? null : DeathItems.encode(stack), previous = before.get(i).item() == null ? null : before.get(i).item().data();
      if (Objects.equals(previous, next)) continue;
      Slot slot = slots.get(i);
      if (stack != null && (!slot.canInsert(stack) || stack.count > Math.min(stack.getMaxCount(), slot.getMaxItemCount())))
        throw new IOException("A native slot rejected this inventory plan. No items changed.");
      var access = (DeathSlotAccessor)(Object)slot;
      changes.add(access.power$deathInventory() == mc.player.inventory ? NativeInventoryPlan.Change.main(mc.player.inventory, access.power$deathIndex(), stack)
          : NativeInventoryPlan.Change.slot(slot, stack));
    }
    return changes;
  }
  public static void feedback(Minecraft mc, String message) {
    if (mc.inGameHud != null) mc.inGameHud.addChatMessage("\u00a7c" + Objects.toString(message, "Inventory action unavailable.") + "\u00a7r");
  }
}
