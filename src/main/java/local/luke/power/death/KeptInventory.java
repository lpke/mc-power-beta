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
  private int format;
  private String offhand;

  public static KeptInventory capture(PlayerEntity player) throws IOException {
    KeptInventory saved = new KeptInventory(); saved.format = 1; saved.selected = player.inventory.selectedSlot;
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
    ItemStack hand = local.luke.power.offhand.Offhand.get(player.inventory);
    if (hand != null && references.put(hand, true) != null) throw new IOException("Shared kept offhand stack");
    saved.offhand = hand == null ? null : DeathItems.encode(hand);
    saved.decode(); return saved;
  }

  public byte[] bytes() { return JSON.toJson(this).getBytes(StandardCharsets.UTF_8); }
  public String offhandData() { return offhand; }
  public static KeptInventory read(byte[] bytes) throws IOException {
    if (bytes.length == 0 || bytes.length > 32 * DeathJournal.MAX_RECORD) throw new IOException("Invalid kept inventory size");
    try {
      var document=com.google.gson.JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
      for(String key:List.of("format","selected"))if(document.has(key)) {
        var value=document.get(key);
        if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())throw new IOException("Invalid kept inventory "+key);
        value.getAsBigDecimal().intValueExact();
      }
      KeptInventory value = JSON.fromJson(document, KeptInventory.class); value.decode(); return value;
    }
    catch (RuntimeException e) { throw new IOException("Invalid kept inventory", e); }
  }
  private ItemStack[] decode() throws IOException {
    if (format < 0 || format > 1 || format == 0 && offhand != null
        || items == null || items.size() != 41 || crafting == null || crafting.size() != 4 || selected < 0 || selected > 8)
      throw new IOException("Invalid kept inventory layout");
    ItemStack[] result = new ItemStack[46];
    for (int i = 0; i < 45; i++) {
      String value = i < 41 ? items.get(i) : crafting.get(i - 41);
      if (value != null) result[i] = DeathItems.decode(value);
    }
    result[45] = offhand == null ? null : DeathItems.decode(offhand);
    return result;
  }
  public boolean matches(PlayerEntity player) throws IOException {
    KeptInventory actual = capture(player);
    return selected == actual.selected && items.equals(actual.items) && crafting.equals(actual.crafting)
        && Objects.equals(offhand, actual.offhand);
  }

  /** Decode every stack before changing any slots; failure restores both inventories. */
  public void restore(PlayerEntity player) throws IOException {
    restore(player, () -> true);
  }
  /** Native player loading truncates main/armor counts to a signed byte before this hook. */
  public void restoreLoaded(PlayerEntity player) throws IOException {
    ItemStack[] verified = decode();
    if (format == 0 && local.luke.power.offhand.Offhand.get(player.inventory) != null)
      throw new IOException("Historical kept snapshot conflicts with an occupied offhand");
    KeptLoadCounts.normalize(player, verified);
    restore(player);
  }
  void restore(PlayerEntity player, DeathInventoryRestore.Admission admitted) throws IOException {
    ItemStack[] stacks = decode(); var inventory = player.inventory;
    var hand = local.luke.power.offhand.Offhand.get(inventory);
    if (format == 0 && hand != null) throw new IOException("Historical kept snapshot conflicts with an occupied offhand");
    var restoration = new DeathInventoryRestore(player, admitted);
    try {
      restoration.publish(stacks, selected);
      if (!matches(player)) throw new IOException("Kept inventory changed during restoration");
    } catch (Exception failure) {
      if (!restoration.rollback(failure)) failure.addSuppressed(new IOException("Kept inventory rollback lost ownership"));
      throw new IOException("Kept inventory restoration failed; saved snapshot retained", failure);
    }
  }
}
