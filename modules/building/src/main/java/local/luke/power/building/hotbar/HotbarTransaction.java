package local.luke.power.building.hotbar;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.UUID;
import local.luke.power.building.config.Config;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

/** All gameplay access stays on the game thread. Any detected failure blocks further swaps. */
final class HotbarTransaction {
  private static boolean busy, failed;
  private static Thread gameThread;
  private static Path journalPath;
  private static RecoveryJournal journal;

  private HotbarTransaction() {}

  static void tick() {
    if (gameThread == null) gameThread = Thread.currentThread();
  }

  static boolean run(Minecraft mc, int row) {
    if (failed
        || busy
        || gameThread == null
        || Thread.currentThread() != gameThread
        || !Hotbars.eligible(mc)) return false;
    busy = true;
    var player = mc.player;
    var inventory = player.inventory;
    var world = mc.world;
    try {
      if (inventory.main == null
          || inventory.main.length != 36
          || inventory.armor == null
          || inventory.armor.length != 4
          || inventory.selectedSlot < 0
          || inventory.selectedSlot > 8)
        throw new IOException("Unexpected player inventory layout");
      IdentityHashMap<ItemStack, Boolean> seen = new IdentityHashMap<>();
      for (ItemStack[] array : new ItemStack[][] {inventory.main, inventory.armor})
        for (ItemStack item : array)
          if (item != null && seen.put(item, Boolean.TRUE) != null)
            throw new IOException("Inventory slots share a stack");
      ItemStack[] source = inventory.main;
      InventorySnapshot before = new InventorySnapshot(source);
      ItemStack[] next = InventoryRows.swapped(source, row);
      InventorySnapshot after = new InventorySnapshot(next);
      InventorySnapshot armorBefore = new InventorySnapshot(inventory.armor);
      int selected = inventory.selectedSlot;
      NbtCompound record = new NbtCompound();
      record.putString("World", world.method_262().getName());
      record.putLong("Seed", world.method_262().getSeed());
      record.putString("Player", player.name);
      record.putLong("Time", System.currentTimeMillis());
      record.putInt("Row", row);
      record.putInt("SelectedSlot", selected);
      record.put("MainBefore", before.list());
      record.put("MainAfter", after.list());
      record.put("Armor", armorBefore.list());
      byte[] bytes = InventorySnapshot.encode(record);
      String identity =
          world.method_262().getName() + "\n" + world.method_262().getSeed() + "\n" + player.name;
      Path path =
          FabricLoader.getInstance()
              .getGameDir()
              .resolve("power-beta-data/hotbar-backups")
              .resolve(
                  UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)) + ".journal");
      if (!path.equals(journalPath)) {
        journalPath = path;
        journal = new RecoveryJournal(path);
      }
      InventoryExchange.commit(
          () -> inventory.main,
          a -> inventory.main = a,
          source,
          before.references,
          next,
          before.rollback,
          () -> {
            journal.append(bytes);
            if (mc.world != world
                || mc.player != player
                || player.inventory != inventory
                || !Hotbars.eligible(mc)) throw new IOException("Player state changed");
            before.verify(inventory.main);
            armorBefore.verify(inventory.armor);
          },
          () -> {
            inventory.dirty = true;
            after.verify(inventory.main);
            armorBefore.verify(inventory.armor);
            if (inventory.getCursorStack() != null)
              throw new IOException("Cursor changed during swap");
            inventory.selectedSlot = selected;
          });
      return true;
    } catch (Exception | LinkageError error) {
      failed = true;
      Config.LOG.error(
          "Hotbar swapping disabled after an inventory protection failure. Recovery journal: "
              + journalPath,
          error);
      mc.inGameHud.addChatMessage(
          "\u00a7cHotbar swap stopped. Inventory recovery snapshots are in power-beta-data/hotbar-backups.\u00a7r");
      return false;
    } finally {
      busy = false;
    }
  }
}
