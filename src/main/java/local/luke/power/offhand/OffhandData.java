package local.luke.power.offhand;

import java.io.*;
import local.luke.power.death.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;

/** Versioned player data. Full counts and native item NBT use the recovery codec. */
public final class OffhandData {
  public static final String TAG = "PowerBetaOffhand";
  public static final int FORMAT = 1;
  private OffhandData() {}
  private static void type(NbtCompound compound, String key, int type) throws IOException {
    for (Object entry : compound.values()) {
      NbtElement element = (NbtElement)entry;
      if (element.getKey().equals(key) && element.getType() == type) return;
    }
    throw new IOException("Invalid offhand field: " + key);
  }
  public static ItemStack read(NbtCompound player) throws IOException {
    if (!player.contains(TAG)) return null;
    type(player, TAG, 10); NbtCompound document = player.getCompound(TAG);
    type(document, "format", 3);
    if (document.getInt("format") != FORMAT) throw new IOException("Unsupported offhand data format. Use the Power Beta version that saved this world.");
    if (!document.contains("stack")) {
      if (document.contains("count")) throw new IOException("Offhand count has no item");
      return null;
    }
    type(document, "stack", 10); type(document, "count", 3);
    NbtCompound stack = copy(document.getCompound("stack"));
    // Nested compounds retain their "stack" key after copying. The recovery codec
    // validates a standalone native item document, whose root key is empty.
    stack.setKey("");
    stack.putInt("PowerBetaFullCount", document.getInt("count"));
    ByteArrayOutputStream bytes = new ByteArrayOutputStream(); NbtIo.write(stack, new DataOutputStream(bytes));
    return DeathItems.decode(java.util.Base64.getEncoder().encodeToString(bytes.toByteArray()));
  }
  public static void write(NbtCompound player, ItemStack stack, NbtCompound previous) throws IOException {
    if (stack == null && previous == null) return;
    NbtCompound document = new NbtCompound();
    if (previous != null) for (Object entry : previous.values()) {
      NbtElement element = (NbtElement)entry;
      if (!java.util.Set.of("format", "stack", "count").contains(element.getKey())) document.put(element.getKey(), element);
    }
    document.putInt("format", FORMAT);
    if (stack != null) {
      // Validate the round trip before publishing player data.
      DeathItems.decode(DeathItems.encode(stack));
      document.put("stack", stack.writeNbt(new NbtCompound())); document.putInt("count", stack.count);
    }
    player.put(TAG, document);
  }
  public static void validatePlayer(NbtCompound player) throws IOException {
    ItemStack ordinary=read(player);
    if (player.contains("PowerBetaKeptInventory")) {
      type(player, "PowerBetaKeptInventory", 7);
      byte[] bytes = player.getByteArray("PowerBetaKeptInventory");
      if (bytes.length != 0) {
        KeptInventory kept=KeptInventory.read(bytes);
        String value=ordinary==null?null:DeathItems.encode(ordinary);
        if(player.contains(TAG)&&!java.util.Objects.equals(value,kept.offhandData()))
          throw new IOException("Conflicting ordinary and kept offhand snapshots; original player data retained");
      }
    }
  }
  public static void validateWorldData(NbtCompound data) throws IOException {
    if (data.contains("Player")) { type(data, "Player", 10); validatePlayer(data.getCompound("Player")); }
  }
  public static NbtCompound copy(NbtCompound document) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream(); NbtIo.write(document, new DataOutputStream(bytes));
    return NbtIo.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
  }
}
