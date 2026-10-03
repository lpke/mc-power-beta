package local.luke.tweaks;

import java.lang.reflect.Method;
import local.luke.fastplace.*;
import local.luke.tweaks.config.Config;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** A pair is one checked block operation and consumes exactly its missing half slabs. */
public final class SlabPairs {
  private static final Method CREATIVE = creativeMethod();

  private static Method creativeMethod() {
    try {
      return Class.forName("paulevs.bhcreative.interfaces.CreativePlayer")
          .getMethod("creative_isCreative");
    } catch (ReflectiveOperationException e) {
      return null;
    }
  }

  public static boolean active(Minecraft mc, ItemStack stack) {
    return configured(stack) && FastPlace.eligible(mc, stack);
  }

  public static boolean configured(ItemStack stack) {
    return stack != null
        && stack.count > 0
        && stack.itemId == 44
        && stack.getDamage() >= 0
        && stack.getDamage() <= 3
        && Config.current().placement.enabled
        && Config.current().placement.slabMode == SlabMode.DOUBLE
        && Config.current().placement.permits(stack.itemId, stack.getDamage());
  }

  private static boolean creative(Object player) {
    try {
      return CREATIVE != null
          && CREATIVE.getDeclaringClass().isInstance(player)
          && Boolean.TRUE.equals(CREATIVE.invoke(player));
    } catch (ReflectiveOperationException e) {
      return false;
    }
  }

  private record Pair(BlockPos position, int cost, boolean creative) {}

  private static Pair plan(Minecraft mc, ItemStack stack, int x, int y, int z, int face) {
    if (!active(mc, stack)) return null;
    World w = mc.world;
    PlacementTarget target = PlacementTarget.nativeAt(w, stack, x, y, z, face);
    if (target == null) return null;
    BlockPos p = target.position();
    boolean nativeTop = face == 1 && p.equals(new BlockPos(x, y, z));
    x = p.x();
    y = p.y();
    z = p.z();
    boolean half = w.getBlockId(x, y, z) == 44 && w.method_1778(x, y, z) == stack.getDamage();
    if (half && !nativeTop && !local.luke.slabplacement.SlabPlacement.enabled()) return null;
    int cost = half ? 1 : 2;
    boolean creative = creative(mc.player);
    if (y >= 127 || stack.count < cost && !creative) return null;
    Block block = Block.BLOCKS[43];
    if (half
        ? !w.canSpawnEntity(block.method_1624(w, x, y, z))
        : !w.method_156(43, x, y, z, false, face)) return null;
    return new Pair(p, cost, creative);
  }

  public static boolean canPlace(Minecraft mc, ItemStack stack, int x, int y, int z, int face) {
    return plan(mc, stack, x, y, z, face) != null;
  }

  public static boolean place(Minecraft mc, ItemStack stack, int x, int y, int z, int face) {
    Pair pair = plan(mc, stack, x, y, z, face);
    if (pair == null) return false;
    World w = mc.world;
    x = pair.position.x();
    y = pair.position.y();
    z = pair.position.z();
    Block block = Block.BLOCKS[43];
    if (!w.method_201(x, y, z, 43, stack.getDamage())) return false;
    w.method_150(
        x + .5,
        y + .5,
        z + .5,
        block.field_1926.method_1978(),
        (block.field_1926.method_1976() + 1) / 2,
        block.field_1926.method_1977() * .8f);
    if (!pair.creative) stack.count -= pair.cost;
    return true;
  }
}
