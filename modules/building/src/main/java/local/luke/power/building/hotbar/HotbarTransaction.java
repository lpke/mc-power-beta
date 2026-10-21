package local.luke.power.building.hotbar;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.concurrent.Future;
import local.luke.power.building.config.Config;
import local.luke.power.inventory.InventoryActions;
import local.luke.power.inventory.OffhandInventory;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

/** Prepare and flush off-thread; only an armed, revalidated plan may publish on the game thread. */
final class HotbarTransaction {
  private static boolean failed;
  private static Thread gameThread;
  private static Path journalPath;
  private static RecoveryJournal journal;
  private static Pending pending;
  private static int previewRow = -1;
  private static final ArrayDeque<Integer> rows = new ArrayDeque<>();
  private static final int MAX_QUEUED = 16;

  private HotbarTransaction() {}

  static void cancel() {
    rows.clear(); previewRow = -1;
    if (pending != null) { pending.cancelled = true; InventoryActions.finish(pending.lease); }
  }

  static void preview(Minecraft mc, int row) {
    if (failed || gameThread != Thread.currentThread() || !Hotbars.eligible(mc) || row < 0 || row > 2) return;
    previewRow = row;
    if (pending == null) try { prepare(mc, row, false); }
    catch (Exception | LinkageError error) { fail(mc, error); }
  }

  static void tick(Minecraft mc) {
    if (gameThread == null) gameThread = Thread.currentThread();
    if (Thread.currentThread() != gameThread || pending == null) return;
    if (!pending.owns(mc)) cancel();
    if (!pending.write.isDone()) return;
    Pending ready = pending;
    try {
      if (!ready.flushed) { ready.write.get(); ready.flushed = true; }
      if (!ready.armed && !ready.cancelled && ready.row == previewRow) return;
      pending = null;
      try {
        if (!ready.cancelled && ready.owns(mc) && ready.armed) {
          if (ready.inventory.main != ready.source || !ready.before.matches(ready.inventory.main)
              || !ready.armor.matches(ready.inventory.armor) || !ready.extra.matches(extra(mc))) {
            rows.clear(); previewRow = -1; return;
          }
          InventoryExchange.commit(
              () -> ready.inventory.main, a -> ready.inventory.main = a,
              ready.source, ready.before.references, ready.after.references, ready.before.rollback,
              () -> {}, () -> ready.inventory.dirty = true);
        }
      } finally { InventoryActions.finish(ready.lease); }
      if (!rows.isEmpty() && Hotbars.eligible(mc)) prepare(mc, rows.removeFirst(), true);
      else if (previewRow >= 0 && Hotbars.eligible(mc)) prepare(mc, previewRow, false);
    } catch (Exception | LinkageError error) {
      if (pending != null) InventoryActions.finish(pending.lease);
      pending = null; fail(mc, error);
    }
  }

  static boolean run(Minecraft mc, int row) {
    if (failed || gameThread == null || Thread.currentThread() != gameThread
        || row < 0 || row > 2 || !Hotbars.eligible(mc)) return false;
    previewRow = -1;
    try {
      if (pending != null) {
        if (pending.cancelled || !pending.owns(mc) || rows.size() >= MAX_QUEUED) return false;
        if (!pending.armed) {
          if (pending.row == row && pending.inventory.main == pending.source
              && pending.before.matches(pending.inventory.main) && pending.armor.matches(pending.inventory.armor)
              && pending.extra.matches(extra(mc))) {
            pending.armed = true; tick(mc); return true;
          }
          pending.cancelled = true;
        }
        rows.addLast(row); return true;
      }
      prepare(mc, row, true); return true;
    } catch (Exception | LinkageError error) { fail(mc, error); return false; }
  }

  private static void prepare(Minecraft mc, int row, boolean armed) throws IOException {
    var player = mc.player;
    var inventory = player.inventory;
    var world = mc.world;
    if (inventory.main == null || inventory.main.length != 36
        || inventory.armor == null || inventory.armor.length != 4
        || inventory.selectedSlot < 0 || inventory.selectedSlot > 8)
      throw new IOException("Unexpected player inventory layout");
    IdentityHashMap<ItemStack, Boolean> seen = new IdentityHashMap<>();
    for (ItemStack[] array : new ItemStack[][] {inventory.main, inventory.armor})
      for (ItemStack item : array)
        if (item != null && seen.put(item, Boolean.TRUE) != null)
          throw new IOException("Inventory slots share a stack");
    ItemStack[] source = inventory.main;
    InventorySnapshot before = new InventorySnapshot(source);
    InventorySnapshot after = before.swapped(row);
    InventorySnapshot armor = new InventorySnapshot(inventory.armor);
    InventorySnapshot extra = new InventorySnapshot(extra(mc));
    for (ItemStack item : extra.references)
      if (item != null && seen.put(item, Boolean.TRUE) != null) throw new IOException("Inventory slots share a stack");
    String worldId = HotbarJournalIdentity.world(world.method_262());
    NbtCompound record = new NbtCompound();
    record.putInt("OwnershipVersion", HotbarJournalIdentity.VERSION);
    record.putString("WorldId", worldId);
    record.putString("World", world.method_262().getName());
    record.putLong("Seed", world.method_262().getSeed());
    record.putString("Player", player.name);
    record.putLong("Time", System.currentTimeMillis());
    record.putInt("Row", row);
    record.putInt("SelectedSlot", inventory.selectedSlot);
    record.put("MainBefore", before.list());
    record.put("MainAfter", after.list());
    record.put("Armor", armor.list());
    record.put("CursorCraftingOffhand", extra.list());
    byte[] bytes = InventorySnapshot.encode(record);
    Path path = HotbarJournalIdentity.path(FabricLoader.getInstance().getGameDir(), worldId, player.name);
    if (!path.equals(journalPath)) {
      journalPath = path;
      journal = new RecoveryJournal(path);
    }
    var lease = InventoryActions.begin(world, player, player.container, inventory, "hotbar_swap", armed);
    if (lease == null) return;
    try {
      pending = new Pending(world, player, inventory, source, before, after, armor, extra, lease,
          JournalWriter.append(journal, bytes), row, armed);
    } catch (java.util.concurrent.RejectedExecutionException busy) { InventoryActions.finish(lease); }
    catch (RuntimeException failure) { InventoryActions.finish(lease); throw failure; }
  }

  @SuppressWarnings("unchecked")
  private static ItemStack[] extra(Minecraft mc) {
    var inventory = mc.player.inventory;
    ItemStack[] items = new ItemStack[6]; items[0] = inventory.getCursorStack();
    for (int i = 0; i < 4; i++) items[i + 1] = mc.player.playerContainer.method_2084(i + 1).getStack();
    items[5] = ((OffhandInventory<ItemStack>)(Object)inventory).power$offhandStack();
    return items;
  }

  private static void fail(Minecraft mc, Throwable error) {
    failed = true;
    cancel();
    Config.LOG.error("Hotbar swapping disabled after an inventory protection failure. Recovery journal: "
        + journalPath, error);
    mc.inGameHud.addChatMessage(
        "\u00a7cHotbar swap stopped. Inventory recovery snapshots are in power-beta-data/hotbar-backups.\u00a7r");
  }

  private static final class Pending {
    final World world;
    final ClientPlayerEntity player;
    final PlayerInventory inventory;
    final ItemStack[] source;
    final InventorySnapshot before, after, armor, extra;
    final InventoryActions.Lease lease;
    final int selected;
    final Future<?> write;
    final int row;
    boolean cancelled, armed, flushed;

    Pending(World world, ClientPlayerEntity player, PlayerInventory inventory, ItemStack[] source,
        InventorySnapshot before, InventorySnapshot after, InventorySnapshot armor, InventorySnapshot extra,
        InventoryActions.Lease lease, Future<?> write, int row, boolean armed) {
      this.world = world;
      this.player = player;
      this.inventory = inventory;
      this.source = source;
      this.before = before;
      this.after = after;
      this.armor = armor;
      this.extra = extra; this.lease = lease; selected = inventory.selectedSlot;
      this.write = write; this.row = row; this.armed = armed;
    }

    boolean owns(Minecraft mc) {
      return mc.world == world && mc.player == player && player.inventory == inventory && inventory.selectedSlot == selected
          && lease.valid(mc.world, mc.player, player.container, player.inventory) && Hotbars.eligible(mc);
    }
  }
}
