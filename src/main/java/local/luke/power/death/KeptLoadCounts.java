package local.luke.power.death;

import java.io.IOException;
import java.util.*;
import local.luke.power.mixin.DeathCraftingAccessor;
import local.luke.power.mixin.DeathSlotAccessor;
import net.minecraft.class_159;
import net.minecraft.class_277;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/** Repairs only signed-byte count projections while loading a fully verified kept snapshot. */
final class KeptLoadCounts {
  private final PlayerEntity player;
  private final Object world;
  private final PlayerInventory inventory;
  private final ItemStack[] main, armor, grid, references;
  private final class_277 container;
  private final class_159 crafting;
  private final int selected;
  private final int[] before, expected, canonical;
  private final String[] data;
  private final List<Integer> corrections = new ArrayList<>();

  static void normalize(PlayerEntity player, ItemStack[] verified) throws IOException {
    ItemStack[] loaded = DeathItems.snapshot(player.inventory, player.playerContainer);
    if (Arrays.stream(loaded).noneMatch(item -> item != null && item.count <= 0)) return;
    new KeptLoadCounts(player, loaded, verified).normalize();
  }

  private KeptLoadCounts(PlayerEntity player, ItemStack[] loaded, ItemStack[] verified) throws IOException {
    this.player = player; world = player.world; inventory = player.inventory;
    main = inventory.main; armor = inventory.armor; selected = inventory.selectedSlot;
    if (verified.length != 46 || player.playerContainer.getClass() != class_277.class)
      throw new IOException("Unsupported native kept-load owner");
    container = (class_277)player.playerContainer; crafting = container.field_1124;
    grid = ((DeathCraftingAccessor)crafting).power$deathStacks();
    for (int i = 1; i <= 4; i++) {
      Slot slot = container.method_2084(i); var owner = (DeathSlotAccessor)slot;
      if (slot.getClass() != Slot.class || owner.power$deathInventory() != crafting || owner.power$deathIndex() != i - 1)
        throw new IOException("Unsupported native kept-load crafting slots");
    }
    references = loaded; before = new int[46]; expected = new int[46]; canonical = new int[46]; data = new String[46];
    var unique = new IdentityHashMap<ItemStack, Boolean>();
    for (int i = 0; i < loaded.length; i++) {
      ItemStack item = loaded[i]; if (item == null) continue;
      if (unique.put(item, true) != null) throw new IOException("Shared native kept-load stack");
      before[i] = expected[i] = canonical[i] = item.count;
      if (item.count <= 0) {
        ItemStack saved = verified[i];
        if (saved == null || saved.count <= 0 || item.count != (byte)saved.count)
          throw new IOException("Native count conflicts with its kept inventory slot");
        canonical[i] = saved.count;
        if (!Objects.equals(bytes(item, saved.count), DeathItems.encode(saved)))
          throw new IOException("Native item data conflicts with its kept inventory slot");
        corrections.add(i);
      }
      data[i] = bytes(item, canonical[i]);
    }
    requireExpected();
  }

  private void normalize() throws IOException {
    var repaired = new ArrayList<Integer>();
    try {
      for (int index : corrections) {
        requireExpected(); references[index].count = expected[index] = canonical[index]; repaired.add(index);
        requireExpected();
      }
    } catch (IOException | RuntimeException | LinkageError cause) {
      IOException failure = cause instanceof IOException io ? io : new IOException("Native kept-load count normalization failed", cause);
      for (int j = repaired.size() - 1; j >= 0; j--) {
        int index = repaired.get(j);
        try {
          if (owns() && DeathItems.snapshot(inventory, container)[index] == references[index]
              && references[index].count == expected[index] && Objects.equals(bytes(references[index], canonical[index]), data[index]))
            references[index].count = before[index];
        } catch (IOException | RuntimeException | LinkageError changed) { failure.addSuppressed(changed); }
      }
      throw failure;
    }
  }
  private boolean owns() {
    return player.world == world && player.inventory == inventory && inventory.main == main && inventory.armor == armor
        && player.playerContainer == container && container.field_1124 == crafting && inventory.selectedSlot == selected
        && ((DeathCraftingAccessor)crafting).power$deathStacks() == grid;
  }
  private void requireExpected() throws IOException {
    if (!owns()) throw new IOException("Native kept-load owner changed");
    ItemStack[] actual = DeathItems.snapshot(inventory, container);
    for (int i = 0; i < actual.length; i++) if (actual[i] != references[i]
        || actual[i] != null && (actual[i].count != expected[i] || !Objects.equals(bytes(actual[i], canonical[i]), data[i])))
      throw new IOException("Native kept-load item changed");
  }
  private static String bytes(ItemStack item, int count) throws IOException {
    ItemStack copy = item.clone(); copy.count = count; return DeathItems.encode(copy);
  }
}
