package local.luke.creative;

import java.util.Arrays;
import local.luke.creative.config.Config;
import net.minecraft.entity.living.player.PlayerEntity;

public final class InventoryActions {
  private InventoryActions() {}

  public static boolean destroy(PlayerEntity p, boolean all) {
    if (p == null || p.level.isRemote || !p.creative_isCreative() || !Config.current().destroySlot)
      return false;
    if (all && Config.current().shiftClearsInventory) {
      Arrays.fill(p.inventory.main, null);
      Arrays.fill(p.inventory.armor, null);
      if (p.playerContainer instanceof net.minecraft.container.PlayerContainer container) {
        for (int i = 0; i < container.craftingInv.getInventorySize(); i++)
          container.craftingInv.setItem(i, null);
        container.resultInv.setItem(0, null);
      }
    }
    p.inventory.setCursorItem(null);
    p.inventory.markDirty();
    return true;
  }
}
