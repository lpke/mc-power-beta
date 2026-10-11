package local.luke.power.death;

import java.io.*;
import java.util.*;
import net.minecraft.Item;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;

/** Native stack snapshots and a complete capacity plan before any inventory writes. */
public final class DeathItems {
  private DeathItems() {}

  public static String encode(ItemStack item) throws IOException {
    if (item == null || item.count <= 0 || item.count > 1_000_000) throw new IOException("Invalid item count");
    NbtCompound tag = item.writeNbt(new NbtCompound());
    tag.putInt("PowerBetaFullCount", item.count);
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    NbtIo.write(tag, new DataOutputStream(bytes));
    if (bytes.size() > DeathJournal.MAX_RECORD / 2) throw new IOException("Item NBT exceeds death snapshot limit");
    return Base64.getEncoder().encodeToString(bytes.toByteArray());
  }

  public static ItemStack decode(String value) throws IOException {
    try {
      byte[] bytes = Base64.getDecoder().decode(value);
      if (bytes.length > DeathJournal.MAX_RECORD / 2) throw new IOException("Item NBT exceeds death snapshot limit");
      NbtCompound tag = NbtIo.read(new DataInputStream(new ByteArrayInputStream(bytes)));
      ItemStack item = new ItemStack(tag); item.count = tag.getInt("PowerBetaFullCount");
      if (item.itemId < 0 || item.itemId >= Item.ITEMS.length || Item.ITEMS[item.itemId] == null
          || item.count <= 0 || item.count > 1_000_000 || item.getMaxCount() <= 0 || !encode(item).equals(value))
        throw new IOException("Saved item cannot be restored without changing its data");
      return item;
    } catch (RuntimeException e) { throw new IOException("Invalid saved item", e); }
  }

  public static void capture(PlayerInventory inventory, DeathRecord record) throws IOException {
    record.items = new ArrayList<>(); record.slots = new ArrayList<>();
    IdentityHashMap<ItemStack, Boolean> references = new IdentityHashMap<>();
    ItemStack[] items = Arrays.copyOf(snapshot(inventory), 41); items[40] = inventory.getCursorStack();
    for (int slot = 0; slot < items.length; slot++) {
      ItemStack item = items[slot];
      if (item != null) {
        if (references.put(item, true) != null) throw new IOException("Shared inventory stack");
        String bytes = encode(item); decode(bytes); record.items.add(bytes); record.slots.add(slot);
      }
    }
  }

  public static ItemStack[] snapshot(PlayerInventory inventory) throws IOException {
    if (inventory.main.length != 36 || inventory.armor.length != 4) throw new IOException("Unsupported inventory layout");
    ItemStack[] result = Arrays.copyOf(inventory.main, 40);
    System.arraycopy(inventory.armor, 0, result, 36, 4); return result;
  }

  public static String inventory(ItemStack[] items) throws IOException {
    List<String> result = new ArrayList<>();
    for (ItemStack item : items) result.add(item == null ? null : encode(item));
    return new com.google.gson.Gson().toJson(result);
  }

  public static ItemStack[] plan(PlayerInventory inventory, net.minecraft.inventory.Container container,
      DeathRecord record, List<Integer> indices) throws IOException {
    ItemStack[] next = snapshot(inventory);
    for (int i = 0; i < next.length; i++) if (next[i] != null) next[i] = decode(encode(next[i]));
    List<ItemStack> items = new ArrayList<>();
    // Any occupied original slot cancels the entire selected recovery, even matching items.
    for (int index : indices) {
      int slot = record.slots.get(index);
      if (slot < 40 && next[slot] != null)
        throw new IllegalArgumentException("Saved inventory slot " + (slot + 1) + " is occupied. Recovery cancelled; no items changed.");
    }
    // Reserve original positions before splitting stacks above the current item limit.
    for (int index : indices) {
      ItemStack item = decode(record.items.get(index)); items.add(item);
      int slot = record.slots.get(index);
      if (slot >= 0 && slot < 36) insert(next, slot, item, inventory.getMaxCountPerStack());
      else if (slot >= 36 && slot < 40) {
        boolean accepted = false;
        if (container == null) throw new IOException("Equipment slots unavailable");
        for (Object entry : container.slots) {
          var nativeSlot = (net.minecraft.screen.slot.Slot)entry;
          var access = (local.luke.power.mixin.DeathSlotAccessor)nativeSlot;
          if (access.power$deathInventory() == inventory && access.power$deathIndex() == slot && nativeSlot.canInsert(item)) {
            insert(next, slot, item, Math.min(1, nativeSlot.getMaxItemCount())); accepted = true; break;
          }
        }
        if (!accepted) throw new IllegalArgumentException("Saved equipment no longer fits its original slot. Recovery cancelled; no items changed.");
      }
    }
    for (ItemStack item : items) {
      for (int i = 0; i < 36 && item.count > 0; i++) if (next[i] != null) insert(next, i, item, inventory.getMaxCountPerStack());
      for (int i = 0; i < 36 && item.count > 0; i++) if (next[i] == null) insert(next, i, item, inventory.getMaxCountPerStack());
      if (item.count > 0) throw new IllegalArgumentException("Not enough inventory space. Recovery cancelled; no items changed.");
    }
    return next;
  }

  private static void insert(ItemStack[] next, int slot, ItemStack item, int limit) throws IOException {
    if (item.count == 0) return;
    ItemStack existing = next[slot];
    if (existing != null && (!item.isStackable() || !sameItem(item, existing))) return;
    int room = Math.max(0, Math.min(item.getMaxCount(), limit) - (existing == null ? 0 : existing.count));
    int amount = Math.min(room, item.count);
    if (amount == 0) return;
    if (existing == null) { next[slot] = decode(encode(item)); next[slot].count = amount; }
    else existing.count += amount;
    item.count -= amount;
  }

  private static boolean sameItem(ItemStack first, ItemStack second) throws IOException {
    ItemStack a = first.clone(), b = second.clone(); a.count = b.count = 1;
    return encode(a).equals(encode(b));
  }
}
