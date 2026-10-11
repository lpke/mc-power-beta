package local.luke.power.death;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/** Full native stacks survive dead-player saves, including counts above NBT's byte limit. */
public final class KeptInventory {
  private static final Gson JSON = new Gson();
  private List<String> items = new ArrayList<>(), crafting = new ArrayList<>();
  private int selected;

  public static KeptInventory capture(PlayerEntity player) throws IOException {
    KeptInventory saved = new KeptInventory(); saved.selected = player.inventory.selectedSlot;
    var references = new IdentityHashMap<ItemStack, Boolean>();
    ItemStack[] items = Arrays.copyOf(DeathItems.snapshot(player.inventory), 41); items[40] = player.inventory.getCursorStack();
    for (ItemStack item : items) {
      if (item != null && references.put(item, true) != null) throw new IOException("Shared kept inventory stack");
      saved.items.add(item == null ? null : DeathItems.encode(item));
    }
    for (int slot = 1; slot <= 4; slot++) {
      ItemStack item = player.playerContainer.method_2084(slot).getStack();
      if (item != null && references.put(item, true) != null) throw new IOException("Shared kept crafting stack");
      saved.crafting.add(item == null ? null : DeathItems.encode(item));
    }
    saved.decode(); return saved;
  }

  public byte[] bytes() { return JSON.toJson(this).getBytes(StandardCharsets.UTF_8); }
  public static KeptInventory read(byte[] bytes) throws IOException {
    if (bytes.length == 0 || bytes.length > 32 * DeathJournal.MAX_RECORD) throw new IOException("Invalid kept inventory size");
    try { KeptInventory value = JSON.fromJson(new String(bytes, StandardCharsets.UTF_8), KeptInventory.class); value.decode(); return value; }
    catch (RuntimeException e) { throw new IOException("Invalid kept inventory", e); }
  }
  private ItemStack[] decode() throws IOException {
    if (items == null || items.size() != 41 || crafting == null || crafting.size() != 4 || selected < 0 || selected > 8)
      throw new IOException("Invalid kept inventory layout");
    ItemStack[] result = new ItemStack[45];
    for (int i = 0; i < 45; i++) {
      String value = i < 41 ? items.get(i) : crafting.get(i - 41);
      if (value != null) result[i] = DeathItems.decode(value);
    }
    return result;
  }
  public boolean matches(PlayerEntity player) throws IOException { return Arrays.equals(bytes(), capture(player).bytes()); }

  /** Decode every stack before changing any slots; failure restores both inventories. */
  public void restore(PlayerEntity player) throws IOException {
    ItemStack[] stacks = decode(); var inventory = player.inventory;
    var main = inventory.main; var armor = inventory.armor; var cursor = inventory.getCursorStack(); int selection = inventory.selectedSlot;
    ItemStack[] craft = new ItemStack[4];
    for (int i = 0; i < 4; i++) craft[i] = player.playerContainer.method_2084(i + 1).getStack();
    try {
      inventory.main = Arrays.copyOf(stacks, 36); inventory.armor = Arrays.copyOfRange(stacks, 36, 40);
      inventory.setCursorStack(stacks[40]); inventory.selectedSlot = selected;
      for (int i = 0; i < 4; i++) player.playerContainer.method_2084(i + 1).setStack(stacks[41 + i]);
      inventory.markDirty();
      if (!matches(player)) throw new IOException("Kept inventory changed during restoration");
    } catch (Exception failure) {
      inventory.main = main; inventory.armor = armor; inventory.setCursorStack(cursor); inventory.selectedSlot = selection;
      for (int i = 0; i < 4; i++) player.playerContainer.method_2084(i + 1).setStack(craft[i]);
      throw new IOException("Kept inventory restoration failed; saved snapshot retained", failure);
    }
  }
}
