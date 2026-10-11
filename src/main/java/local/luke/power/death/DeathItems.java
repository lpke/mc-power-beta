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

  public static List<String> lost(PlayerInventory inventory) throws IOException {
    List<String> result = new ArrayList<>();
    IdentityHashMap<ItemStack, Boolean> references = new IdentityHashMap<>();
    for (ItemStack[] section : new ItemStack[][]{inventory.main, inventory.armor, {inventory.getCursorStack()}})
      for (ItemStack item : section) if (item != null) {
        if (references.put(item, true) != null) throw new IOException("Shared inventory stack");
        String bytes = encode(item); decode(bytes); result.add(bytes);
      }
    return result;
  }

  public static String inventory(ItemStack[] items) throws IOException {
    List<String> result = new ArrayList<>();
    for (ItemStack item : items) result.add(item == null ? null : encode(item));
    return new com.google.gson.Gson().toJson(result);
  }

  public static ItemStack[] plan(PlayerInventory inventory, List<String> lost) throws IOException {
    ItemStack[] next = new ItemStack[inventory.main.length];
    for (int i = 0; i < next.length; i++) if (inventory.main[i] != null) next[i] = decode(encode(inventory.main[i]));
    for (String bytes : lost) {
      ItemStack item = decode(bytes);
      for (int i = 0; i < next.length && item.count > 0; i++) {
        ItemStack existing = next[i];
        if (existing == null || !item.isStackable() || !sameItem(item, existing)) continue;
        int room = Math.max(0, Math.min(existing.getMaxCount(), inventory.getMaxCountPerStack()) - existing.count);
        int amount = Math.min(room, item.count); existing.count += amount; item.count -= amount;
      }
      for (int i = 0; i < next.length && item.count > 0; i++) if (next[i] == null) {
        int amount = Math.min(item.count, Math.min(item.getMaxCount(), inventory.getMaxCountPerStack()));
        if (amount <= 0) throw new IOException("Invalid inventory stack limit");
        next[i] = decode(bytes); next[i].count = amount; item.count -= amount;
      }
      if (item.count > 0) throw new IllegalArgumentException("Not enough inventory space. Recovery cancelled; no items changed.");
    }
    return next;
  }

  private static boolean sameItem(ItemStack first, ItemStack second) throws IOException {
    ItemStack a = first.clone(), b = second.clone(); a.count = b.count = 1;
    return encode(a).equals(encode(b));
  }
}
