package local.luke.building.validation;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Explicit fault injection, active only during the disposable-clone inventory test. */
public final class ItemUseFault {
  public static boolean enabled, throwing;
  private static int depth;

  public static boolean use(ItemStack stack, PlayerEntity player, World world) {
    depth++;
    try {
      stack.count--;
      if (throwing) throw new IllegalStateException("Injected item-use failure");
      if (depth == 1) {
        ItemStack nested = new ItemStack(280, 12, 0);
        nested.method_701(player, world, 0, 100, 0, 1);
        ValidationRun.check(nested.count == 12, "nested stack restored independently");
      }
      return false;
    } finally {
      depth--;
    }
  }
}
