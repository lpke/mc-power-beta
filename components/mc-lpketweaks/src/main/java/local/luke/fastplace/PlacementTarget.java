package local.luke.fastplace;

import local.luke.fastplace.mixin.BlockItemAccessor;
import net.minecraft.class_533;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Predicts only the destination; the game's item code still decides whether/how to place. */
public record PlacementTarget(
    BlockPos position, int expectedBlock, int previousId, int previousMeta) {
  public static int placedBlock(ItemStack stack) {
    if (stack == null
        || stack.count <= 0
        || stack.itemId < 0
        || stack.itemId >= net.minecraft.Item.ITEMS.length) return -1;
    if (stack.getItem() instanceof class_533 item)
      return ((BlockItemAccessor) item).fastplace$blockId();
    return switch (stack.itemId) {
      case 331 -> 55; // Redstone dust
      case 356 -> 93; // Repeater
      case 324 -> 64; // Wooden door
      case 330 -> 71; // Iron door
      case 355 -> 26; // Bed
      case 323 -> 63; // Standing or wall sign; its editor stops further placement
      case 338 -> 83; // Sugar cane
      case 295 -> 59; // Seeds
      case 354 -> 92; // Cake
      default -> -1; // Buckets, vehicles, food and editor tools never repeat
    };
  }

  public static PlacementTarget at(World world, ItemStack stack, int x, int y, int z, int face) {
    if (face < 0
        || face > 5
        || y < 0
        || y >= 128
        || Math.abs((long) x) >= 32000000
        || Math.abs((long) z) >= 32000000) return null;
    int[] adjusted = Compatibility.flexibleClick(world, stack, x, y, z, face);
    if (adjusted != null) {
      if (adjusted.length != 4) return null;
      x = adjusted[0];
      y = adjusted[1];
      z = adjusted[2];
      face = adjusted[3];
    }
    return nativeAt(world, stack, x, y, z, face);
  }

  public static PlacementTarget nativeAt(
      World world, ItemStack stack, int x, int y, int z, int face) {
    int[] merge = Compatibility.slabTarget(world, stack, x, y, z, face);
    if (merge != null)
      return new PlacementTarget(
          new BlockPos(merge[0], merge[1], merge[2]),
          43,
          44,
          world.method_1778(merge[0], merge[1], merge[2]));
    int expected = placedBlock(stack);
    if (expected < 0 || !world.method_239(x, y, z)) return null;
    int hit = world.getBlockId(x, y, z);
    BlockPos position = new BlockPos(x, y, z);
    boolean slabMerge =
        expected == 44 && hit == 44 && face == 1 && world.method_1778(x, y, z) == stack.getDamage();
    boolean replace =
        expected != 59
            && (hit == 78 || (Compatibility.replacesPlants() && (hit == 31 || hit == 32)));
    if (slabMerge) expected = 43;
    else if (!replace) position = position.offset(face);
    if (expected == 59) position = new BlockPos(x, y + 1, z);
    if (expected == 63 && face != 1) expected = 68;
    if (position.y() < 0
        || position.y() >= 128
        || !world.method_239(position.x(), position.y(), position.z())) return null;
    if (expected == 44 && local.luke.tweaks.SlabPairs.configured(stack)) expected = 43;
    return new PlacementTarget(
        position,
        expected,
        world.getBlockId(position.x(), position.y(), position.z()),
        world.method_1778(position.x(), position.y(), position.z()));
  }

  public boolean changedAsExpected(World world) {
    int id = world.getBlockId(position.x(), position.y(), position.z());
    int meta = world.method_1778(position.x(), position.y(), position.z());
    return (id != previousId || meta != previousMeta)
        && (id == expectedBlock || (expectedBlock == 93 && id == 94));
  }
}
