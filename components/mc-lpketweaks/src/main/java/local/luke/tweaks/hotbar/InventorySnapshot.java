package local.luke.tweaks.hotbar;

import java.io.*;
import java.util.Arrays;
import java.util.IdentityHashMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;

/** Native NBT round trips must preserve every stack before a swap is permitted. */
final class InventorySnapshot {
  final ItemStack[] references, rollback;
  final byte[][] data;

  InventorySnapshot(ItemStack[] source) throws IOException {
    references = source.clone();
    rollback = new ItemStack[source.length];
    data = new byte[source.length][];
    IdentityHashMap<ItemStack, Boolean> unique = new IdentityHashMap<>();
    for (int i = 0; i < source.length; i++) {
      ItemStack item = source[i];
      if (item == null) continue;
      if (item.count <= 0 || unique.put(item, Boolean.TRUE) != null)
        throw new IOException("Invalid or shared inventory stack");
      data[i] = encode(item.writeNbt(new NbtCompound()));
      ItemStack restored = new ItemStack(decode(data[i]));
      if (restored.count != item.count
          || restored.itemId != item.itemId
          || restored.getDamage() != item.getDamage()
          || !Arrays.equals(data[i], encode(restored.writeNbt(new NbtCompound()))))
        throw new IOException("A stack cannot be backed up without changing its data");
      rollback[i] = restored;
    }
  }

  void verify(ItemStack[] live) throws IOException {
    if (!InventoryRows.identical(live, references))
      throw new IOException("Inventory references changed");
    for (int i = 0; i < live.length; i++)
      if (live[i] != null && !Arrays.equals(data[i], encode(live[i].writeNbt(new NbtCompound()))))
        throw new IOException("Inventory contents changed");
  }

  NbtList list() throws IOException {
    NbtList list = new NbtList();
    for (int i = 0; i < data.length; i++)
      if (data[i] != null) {
        NbtCompound tag = decode(data[i]);
        tag.putByte("Slot", (byte) i);
        list.add(tag);
      }
    return list;
  }

  static byte[] encode(NbtCompound tag) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (DataOutputStream out =
        new DataOutputStream(
            new FilterOutputStream(bytes) {
              private int count;

              @Override
              public void write(int b) throws IOException {
                limit(1);
                out.write(b);
              }

              @Override
              public void write(byte[] b, int off, int len) throws IOException {
                limit(len);
                out.write(b, off, len);
              }

              private void limit(int size) throws IOException {
                if (size > RecoveryJournal.MAX_RECORD - count)
                  throw new IOException("Inventory NBT exceeds size limit");
                count += size;
              }
            })) {
      NbtIo.write(tag, out);
    }
    return bytes.toByteArray();
  }

  static NbtCompound decode(byte[] data) throws IOException {
    return NbtIo.read(new DataInputStream(new ByteArrayInputStream(data)));
  }
}
