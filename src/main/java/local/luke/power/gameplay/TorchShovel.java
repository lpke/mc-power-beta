package local.luke.power.gameplay;

import local.luke.power.input.PlayerMode;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/** A bounded inventory lookup on mining only. The selected slot and held stack never change. */
public final class TorchShovel {
  private TorchShovel() {}
  public static int slot(PlayerEntity player, Block block) {
    if (!GameplayConfig.current().torchShovel || block != Block.SAND && block != Block.GRAVEL
        || player == null || player.world == null || player.world.isRemote || player.dead || player.health <= 0
        || player instanceof PlayerMode mode && (mode.power$creative() || mode.power$spectator())) return -1;
    ItemStack held = player.method_502();
    if (held == null || held.count <= 0 || held.itemId != 50) return -1;
    int selected = -1, damage = -1;
    ItemStack[] main = player.inventory.main;
    for (int i = 0; i < main.length; i++) {
      ItemStack stack = main[i];
      if (stack != null && stack.itemId == 277 && stack.count > 0
          && stack.getDamage() >= 0 && stack.getDamage() <= stack.getMaxDamage() && stack.getDamage() > damage) {
        selected = i; damage = stack.getDamage();
      }
    }
    return selected;
  }

  public static void wear(PlayerEntity player, Block block, int slot, ItemStack shovel, int x, int y, int z) {
    // Block callbacks may change inventories or settings. Charge only the same, still-owned tool.
    if (slot < 0 || slot >= player.inventory.main.length || player.inventory.main[slot] != shovel
        || slot(player, block) != slot) return;
    shovel.method_696(block.id, x, y, z, player);
    if (shovel.count <= 0 && player.inventory.main[slot] == shovel) {
      shovel.method_700(player);
      player.inventory.main[slot] = null;
    }
    player.inventory.markDirty();
  }
}
