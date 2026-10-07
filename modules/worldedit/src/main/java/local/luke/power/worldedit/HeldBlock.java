package local.luke.power.worldedit;

import local.luke.power.worldedit.core.BlockValue;
import local.luke.power.worldedit.mixin.BlockItemAccessor;
import net.minecraft.class_533;
import net.minecraft.item.ItemStack;

/** Resolves a block item without placing it or changing the stack. */
public final class HeldBlock {
  private HeldBlock() {}

  public static BlockValue read(ItemStack stack) {
    if (stack == null || stack.count <= 0 || !(stack.getItem() instanceof class_533 item))
      throw new IllegalArgumentException("Hold a block to use hand.");
    return new BlockValue(((BlockItemAccessor) item).worldedit$blockId(), item.method_470(stack.getDamage()));
  }
}
