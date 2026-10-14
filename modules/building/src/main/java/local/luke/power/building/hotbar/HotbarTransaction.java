package local.luke.power.building.hotbar;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.UUID;
import java.util.concurrent.Future;
import local.luke.power.building.config.Config;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

/** Flush off-thread, then revalidate and publish on a later game tick. */
final class HotbarTransaction {
  private static boolean failed;
  private static Thread gameThread;
  private static Path journalPath;
  private static RecoveryJournal journal;
  private static Pending pending;
  private static final ArrayDeque<Integer> rows = new ArrayDeque<>();
  private static final int MAX_QUEUED = 16;

  private HotbarTransaction() {}

  static void cancel() {
    rows.clear();
    if (pending != null) pending.cancelled = true;
  }

  static void tick(Minecraft mc) {
    if (gameThread == null) gameThread = Thread.currentThread();
    if (Thread.currentThread() != gameThread) return;
    if (pending == null) return;
    if (!pending.owns(mc)) cancel();
    if (!pending.write.isDone()) return;
    Pending ready = pending;
    pending = null;
    try {
      // isDone makes this non-blocking. Even a cancelled request must report disk failures.
      ready.write.get();
      if (ready.cancelled || !ready.owns(mc)
          || ready.inventory.main != ready.source
          || !ready.before.matches(ready.inventory.main)
          || !ready.armor.matches(ready.inventory.armor)) {
        rows.clear();
        return;
      }
      InventoryExchange.commit(
          () -> ready.inventory.main, a -> ready.inventory.main = a,
          ready.source, ready.before.references, ready.after.references, ready.before.rollback,
          () -> {}, () -> ready.inventory.dirty = true);
      if (!rows.isEmpty()) prepare(mc, rows.removeFirst());
    } catch (Exception | LinkageError error) {
      fail(mc, error);
    }
  }

  static boolean run(Minecraft mc, int row) {
    if (failed || gameThread == null || Thread.currentThread() != gameThread
        || row < 0 || row > 2 || !Hotbars.eligible(mc)) return false;
    if (pending != null) {
      if (pending.cancelled || !pending.owns(mc) || rows.size() >= MAX_QUEUED) return false;
      rows.addLast(row);
      return true;
    }
    try {
      prepare(mc, row);
      return true;
    } catch (Exception | LinkageError error) {
      fail(mc, error);
      return false;
    }
  }

  private static void prepare(Minecraft mc, int row) throws IOException {
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
    NbtCompound record = new NbtCompound();
    record.putString("World", world.method_262().getName());
    record.putLong("Seed", world.method_262().getSeed());
    record.putString("Player", player.name);
    record.putLong("Time", System.currentTimeMillis());
    record.putInt("Row", row);
    record.putInt("SelectedSlot", inventory.selectedSlot);
    record.put("MainBefore", before.list());
    record.put("MainAfter", after.list());
    record.put("Armor", armor.list());
    byte[] bytes = InventorySnapshot.encode(record);
    String identity = world.method_262().getName() + "\n" + world.method_262().getSeed() + "\n" + player.name;
    Path path = FabricLoader.getInstance().getGameDir().resolve("power-beta-data/hotbar-backups")
        .resolve(UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)) + ".journal");
    if (!path.equals(journalPath)) {
      journalPath = path;
      journal = new RecoveryJournal(path);
    }
    pending = new Pending(world, player, inventory, source, before, after, armor,
        JournalWriter.append(journal, bytes));
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
    final InventorySnapshot before, after, armor;
    final Future<?> write;
    boolean cancelled;

    Pending(World world, ClientPlayerEntity player, PlayerInventory inventory, ItemStack[] source,
        InventorySnapshot before, InventorySnapshot after, InventorySnapshot armor, Future<?> write) {
      this.world = world;
      this.player = player;
      this.inventory = inventory;
      this.source = source;
      this.before = before;
      this.after = after;
      this.armor = armor;
      this.write = write;
    }

    boolean owns(Minecraft mc) {
      return mc.world == world && mc.player == player && player.inventory == inventory && Hotbars.eligible(mc);
    }
  }
}
