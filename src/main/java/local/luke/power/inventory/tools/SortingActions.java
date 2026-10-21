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
  public static boolean sortAtPointer(Minecraft mc) {
    if (mc == null || mc.currentScreen == null || mc.displayWidth <= 0 || mc.displayHeight <= 0) return false;
    return sortAt(mc, org.lwjgl.input.Mouse.getX() * mc.currentScreen.width / mc.displayWidth,
        mc.currentScreen.height - org.lwjgl.input.Mouse.getY() * mc.currentScreen.height / mc.displayHeight - 1);
  }
  /** A hotkey follows the player region; every other pointer position targets the open container. */
  public static boolean sortAt(Minecraft mc, int x, int y) {
    if (mc == null || !(mc.currentScreen instanceof InventoryToolsHost host)) return false;
    var player = host.power$inventoryToolsPlayerArea();
    return sort(mc, player != null && player.contains(x, y));
  }
  public static boolean sort(Minecraft mc) { return sort(mc, false); }
  public static boolean sort(Minecraft mc, boolean playerStorage) {
    if (!InventoryToolsConfig.current().sorting || !SortingSlots.eligible(mc)) return false;
    try {
      var regions = SortingSlots.read(mc); List<Slot> slots = playerStorage || regions.storage().isEmpty() ? regions.player() : regions.storage();
      var before = capture(mc, slots);
      Map<Integer, SortingPlanner.Preference> preferences = new LinkedHashMap<>();
      if (slots == regions.player()) for (int i = 0; i < slots.size(); i++) {
        int nativeIndex = ((DeathSlotAccessor)(Object)slots.get(i)).power$deathIndex();
        String value = InventoryToolsConfig.current().preferredItems.get(nativeIndex);
        if (value != null) {
          var item = local.luke.power.ui.ItemCatalogue.resolve(local.luke.power.ui.ItemCatalogue.Domain.FILTER, value);
          preferences.put(i, new SortingPlanner.Preference(item.id(), item.damage()));
        }
      }
      var after = SortingPlanner.sort(before, InventoryToolsConfig.current().sortDescending, preferences);
      List<NativeInventoryPlan.Change> changes = changes(mc, slots, before, after);
      return NativeInventoryPlan.submit(mc, "inventory_sort", true, changes,
          () -> InventoryToolsConfig.current().sorting && SortingSlots.owns(mc, regions), () -> {});
    } catch (IOException | RuntimeException failure) { feedback(mc, failure.getMessage()); return false; }
  }
  public static boolean depositMatching(Minecraft mc) {
    if (!InventoryToolsConfig.current().depositMatching || !SortingSlots.eligible(mc)) return false;
    try {
      var regions = SortingSlots.read(mc); if (regions.storage().isEmpty()) return false;
      var source = capture(mc, regions.player()); var destination = capture(mc, regions.storage()); var planned = SortingPlanner.deposit(source, destination);
      List<NativeInventoryPlan.Change> changes = new ArrayList<>(changes(mc, regions.player(), source, planned.source()));
      changes.addAll(changes(mc, regions.storage(), destination, planned.destination()));
      return NativeInventoryPlan.submit(mc, "deposit_matching", true, changes,
          () -> InventoryToolsConfig.current().depositMatching && SortingSlots.owns(mc, regions), () -> {});
    } catch (IOException | RuntimeException failure) { feedback(mc, failure.getMessage()); return false; }
  }
  private static List<SortingPlanner.Cell> capture(Minecraft mc, List<Slot> slots) throws IOException {
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
      var access = (DeathSlotAccessor)(Object)slot;
      pinned |= access.power$deathInventory() == mc.player.inventory
          && InventoryToolsConfig.current().protectedSlots.contains(access.power$deathIndex());
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
