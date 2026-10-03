package local.luke.slabplacement;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public final class SlabMerge {
  private static final int[][] OFFSETS = {
    {0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}
  };

  private SlabMerge() {}

  public static int[] target(World world, ItemStack stack, int x, int y, int z, int face) {
    if (!SlabPlacement.enabled()
        || world == null
        || world.isRemote
        || stack == null
        || stack.count <= 0
        || stack.itemId != 44
        || stack.getDamage() < 0
        || stack.getDamage() > 3
        || face < 0
        || face > 5
        || !loaded(world, x, y, z)) return null;
    if (face == 1 && matches(world, stack, x, y, z)) return new int[] {x, y, z};
    int[] d = OFFSETS[face];
    x += d[0];
    y += d[1];
    z += d[2];
    return loaded(world, x, y, z) && matches(world, stack, x, y, z) ? new int[] {x, y, z} : null;
  }

  private static boolean loaded(World w, int x, int y, int z) {
    return y >= 0
        && y < 128
        && Math.abs((long) x) < 32000000
        && Math.abs((long) z) < 32000000
        && w.method_239(x, y, z);
  }

  private static boolean matches(World w, ItemStack s, int x, int y, int z) {
    return w.getBlockId(x, y, z) == 44 && w.method_1778(x, y, z) == s.getDamage();
  }

  /** Called inside native ItemStack use, retaining BHCreative's inventory handling. */
  public static boolean complete(World w, ItemStack stack, int[] p) {
    int x = p[0], y = p[1], z = p[2];
    if (!matches(w, stack, x, y, z) || y == 127 || stack.count <= 0) return false;
    Block full = Block.BLOCKS[43];
    if (!w.canSpawnEntity(full.method_1624(w, x, y, z))) return false;
    if (!w.method_201(x, y, z, 43, stack.getDamage())) return false;
    w.method_150(
        x + .5,
        y + .5,
        z + .5,
        full.field_1926.method_1978(),
        (full.field_1926.method_1976() + 1) / 2,
        full.field_1926.method_1977() * .8f);
    --stack.count;
    return true;
  }
}
