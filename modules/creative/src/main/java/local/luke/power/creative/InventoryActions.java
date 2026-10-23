package local.luke.power.creative;

import local.luke.power.creative.config.Config;
import net.minecraft.entity.living.player.PlayerEntity;

public final class InventoryActions {
  private InventoryActions() {}

  /** Insert at most one item-limit stack, merging matching slots before filling empty ones. */
  public static boolean catalogueStack(PlayerEntity player, net.minecraft.item.ItemStack template) {
    if (player == null || player.level.isRemote || !Modes.creativeInventory(player)
        || !local.luke.power.rules.WorldRules.enabled(player.level.getProperties(), local.luke.power.rules.GameRule.ALLOW_CREATIVE_MODE) || template == null) return false;
    var inventory = player.inventory;
    var incoming = template.copy();
    incoming.count = Math.min(incoming.getMaxStackSize(), inventory.getMaxStackSize());
    int remaining = incoming.count;
    var before = java.util.Arrays.stream(inventory.main).map(item -> net.minecraft.item.ItemStack.copy(item))
        .toArray(net.minecraft.item.ItemStack[]::new);
    var after = java.util.Arrays.stream(inventory.main).map(item -> net.minecraft.item.ItemStack.copy(item))
        .toArray(net.minecraft.item.ItemStack[]::new);
    for (boolean empty : new boolean[] {false, true}) for (int slot = 0; slot < after.length && remaining > 0; slot++) {
      var existing = after[slot];
      if ((existing == null) != empty) continue;
      if (existing != null && (!incoming.isStackable() || !incoming.isDamageAndIDIdentical(existing))) continue;
      int count = Math.min(remaining, incoming.count - (existing == null ? 0 : existing.count));
      if (count <= 0) continue;
      if (existing == null) { existing = incoming.copy(); existing.count = 0; after[slot] = existing; }
      existing.count += count;
      remaining -= count;
    }
    if (remaining == incoming.count) return false;
    try {
      System.arraycopy(after, 0, inventory.main, 0, after.length);
      inventory.markDirty();
    } catch (RuntimeException error) {
      System.arraycopy(before, 0, inventory.main, 0, before.length);
      throw error;
    }
    return true;
  }

  public static boolean destroy(PlayerEntity p, boolean all) {
    if (p == null || p.level.isRemote || !Modes.creativeInventory(p) || !Config.current().destroySlot)
      return false;
    Object client = net.fabricmc.loader.api.FabricLoader.getInstance().getGameInstance();
    if (!(client instanceof net.minecraft.client.Minecraft mc) || mc.player != p) return false;
    return local.luke.power.input.CreativePaletteBridge.destroy(client, all && Config.current().shiftClearsInventory);
  }
}
