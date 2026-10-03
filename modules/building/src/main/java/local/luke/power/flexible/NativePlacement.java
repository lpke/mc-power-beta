package local.luke.power.flexible;

import local.luke.power.flexible.mixin.BlockItemAccessor;
import net.minecraft.class_533;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Predicts the vanilla/addon destination without changing the world. */
public final class NativePlacement {
  public record Target(BlockPos pos, int block) {}

  private NativePlacement() {}

  public static int block(ItemStack s) {
    if (s == null || s.count <= 0 || s.itemId < 0 || s.itemId >= net.minecraft.Item.ITEMS.length)
      return -1;
    if (s.getItem() instanceof class_533 item) return ((BlockItemAccessor) item).flexible$blockId();
    return s.itemId == 356 ? 93 : -1;
  }

  public static boolean loaded(World w, BlockPos p) {
    return p.inBounds() && w.method_239(p.x(), p.y(), p.z());
  }

  public static Target target(World w, ItemStack s, BlockPos clicked, Direction face) {
    int expected = block(s);
    if (expected < 0 || !loaded(w, clicked)) return null;
    int hit = w.getBlockId(clicked.x(), clicked.y(), clicked.z());
    boolean merge =
        expected == 44
            && hit == 44
            && face == Direction.UP
            && w.method_1778(clicked.x(), clicked.y(), clicked.z()) == s.getDamage();
    boolean replace = hit == 78 || (Compatibility.replacesPlants() && (hit == 31 || hit == 32));
    BlockPos pos = merge || replace ? clicked : clicked.offset(face);
    if (!loaded(w, pos)) return null;
    if (merge) expected = 43;
    if (expected == 44
        && Compatibility.adjacentSlabs()
        && w.getBlockId(pos.x(), pos.y(), pos.z()) == 44
        && w.method_1778(pos.x(), pos.y(), pos.z()) == s.getDamage()) expected = 43;
    if (expected == 44 && local.luke.power.building.SlabPairs.configured(s)) expected = 43;
    return new Target(pos, expected);
  }
}
