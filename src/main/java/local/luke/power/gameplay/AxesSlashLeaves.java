package local.luke.power.gameplay;

import local.luke.power.input.PlayerMode;
import net.minecraft.Item;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.class_420;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/** Changes mining strength only. Native harvesting owns drops, tool wear and break cleanup. */
public final class AxesSlashLeaves {
  private AxesSlashLeaves() {}

  public static float speed(PlayerEntity player, Block block, float ordinary) {
    if (!GameplayConfig.current().axesSlashLeaves || block == null || block.field_1900 != Material.LEAVES
        || player == null || player.world == null || player.world.isRemote || player.dead || player.health <= 0
        || player instanceof PlayerMode mode && (mode.power$creative() || mode.power$spectator())
        || !Float.isFinite(ordinary) || ordinary <= 0) return ordinary;
    ItemStack held = player.method_502();
    if (held == null || held.count <= 0 || held.itemId < 0 || held.itemId >= Item.ITEMS.length
        || !(Item.ITEMS[held.itemId] instanceof class_420)) return ordinary;
    float axeSpeed = held.method_708(Block.LOG);
    if (!Float.isFinite(axeSpeed) || axeSpeed <= 0) return ordinary;
    // Native leaves: wood/stone/iron/diamond/gold take 4/3/3/2/2 progress ticks.
    // Diamond shovels also take 2 progress ticks on sand. Gold stays below an instant break.
    return Math.max(ordinary, 1 + axeSpeed * .3F);
  }
}
