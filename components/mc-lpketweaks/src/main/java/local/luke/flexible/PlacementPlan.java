package local.luke.flexible;

import net.minecraft.block.Block;
import net.minecraft.class_212;
import net.minecraft.class_27;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

public record PlacementPlan(
    BlockPos clicked,
    Direction face,
    BlockPos destination,
    int block,
    Direction facing,
    boolean reverse,
    boolean valid,
    FaceGrid.Part part) {
  public static PlacementPlan create(
      Minecraft mc, ItemStack stack, int x, int y, int z, int side, Modes modes) {
    if (side < 0 || side > 5 || NativePlacement.block(stack) < 0) return null;
    BlockPos source = new BlockPos(x, y, z);
    if (!NativePlacement.loaded(mc.world, source)) return null;
    class_27 hit = mc.field_2823;
    // Use the real entity-aware raycast. Never infer a target behind an entity or outside reach.
    if (hit == null
        || hit.field_1983 != class_212.TILE
        || hit.field_1988 == null
        || hit.field_1984 != x
        || hit.field_1985 != y
        || hit.field_1986 != z
        || hit.field_1987 != side) return null;
    double hx = hit.field_1988.x, hy = hit.field_1988.y, hz = hit.field_1988.z;
    if (!Double.isFinite(hx) || !Double.isFinite(hy) || !Double.isFinite(hz)) return null;
    double reach = mc.interactionManager.method_1715();
    if (distance(mc, hx, hy, hz) > reach * reach + .01) return null;
    Direction face = Direction.values()[side], forward = Direction.horizontal(mc.player.yaw);
    double[] uv = FaceGrid.uv(face, forward, hx - x, hy - y, hz - z);
    FaceGrid.Part part = FaceGrid.part(uv[0], uv[1]);
    Direction selection = FaceGrid.selected(face, forward, part);
    NativePlacement.Target normal = NativePlacement.target(mc.world, stack, source, face);
    if (normal == null) return null;
    BlockPos destination = normal.pos();
    if (modes.adjacent() && part != FaceGrid.Part.CENTER)
      destination = destination.offset(face.opposite()).offset(selection);
    if (modes.offset()) destination = destination.offset(selection);
    Direction facing = modes.into() ? face.opposite() : modes.rotation() ? selection : null;
    int block = NativePlacement.block(stack);
    Direction placeFace = modes.rotation() && !Orientation.directional(block) ? selection : face;
    BlockPos anchor = source;
    boolean moved = !destination.equals(normal.pos()) || placeFace != face;
    if (moved) {
      anchor = destination.offset(placeFace.opposite());
      NativePlacement.Target predicted = NativePlacement.target(mc.world, stack, anchor, placeFace);
      // Snow/plants and slab merges may consume the synthetic anchor instead of offsetting it.
      // Try other click faces only for blocks whose placement does not attach to that face.
      if (predicted == null || !predicted.pos().equals(destination)) {
        if (!attached(block))
          for (Direction candidate : Direction.values()) {
            BlockPos at = destination.offset(candidate.opposite());
            NativePlacement.Target t = NativePlacement.target(mc.world, stack, at, candidate);
            if (t != null && t.pos().equals(destination)) {
              anchor = at;
              placeFace = candidate;
              predicted = t;
              break;
            }
          }
        if (predicted == null || !predicted.pos().equals(destination))
          return new PlacementPlan(
              anchor, placeFace, destination, block, facing, modes.reverse(), false, part);
      }
    }
    NativePlacement.Target target = NativePlacement.target(mc.world, stack, anchor, placeFace);
    boolean valid =
        target != null
            && target.pos().equals(destination)
            && Orientation.valid(block, facing)
            && withinReach(mc, destination, reach)
            && canPlace(mc, stack, target, anchor, placeFace);
    return new PlacementPlan(
        anchor,
        placeFace,
        destination,
        target == null ? block : target.block(),
        facing,
        modes.reverse(),
        valid,
        part);
  }

  private static boolean attached(int block) {
    return switch (block) {
      case 50, 65, 68, 69, 75, 76, 77, 96 -> true;
      default -> false;
    };
  }

  private static boolean canPlace(
      Minecraft mc,
      ItemStack stack,
      NativePlacement.Target target,
      BlockPos anchor,
      Direction face) {
    if (local.luke.tweaks.SlabPairs.active(mc, stack))
      return local.luke.tweaks.SlabPairs.canPlace(
          mc, stack, anchor.x(), anchor.y(), anchor.z(), face.ordinal());
    BlockPos p = target.pos();
    if (!NativePlacement.loaded(mc.world, p)) return false;
    if (p.y() == 127 && Block.BLOCKS[target.block()].field_1900.method_905()) return false;
    if (target.block() == 43 && mc.world.getBlockId(p.x(), p.y(), p.z()) == 44)
      return mc.world.canSpawnEntity(Block.BLOCKS[43].method_1624(mc.world, p.x(), p.y(), p.z()));
    return mc.world.method_156(target.block(), p.x(), p.y(), p.z(), false, face.ordinal());
  }

  private static double distance(Minecraft mc, double x, double y, double z) {
    double dx = x - mc.player.x, dy = y - mc.player.y, dz = z - mc.player.z;
    return dx * dx + dy * dy + dz * dz;
  }

  private static boolean withinReach(Minecraft mc, BlockPos p, double reach) {
    double x = Math.max(p.x(), Math.min(p.x() + 1, mc.player.x));
    double y = Math.max(p.y(), Math.min(p.y() + 1, mc.player.y));
    double z = Math.max(p.z(), Math.min(p.z() + 1, mc.player.z));
    return distance(mc, x, y, z) <= reach * reach;
  }
}
