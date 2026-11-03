package local.luke.power.gameplay;

import java.util.ArrayList;
import java.util.List;
import local.luke.power.input.PlayerMode;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public final class TrapdoorClimbing {
  private TrapdoorClimbing() {}

  public static boolean enabled(Entity entity) {
    return !entity.world.isRemote && GameplayConfig.current().modernTrapdoorClimbing
        && !(entity instanceof PlayerMode mode && mode.power$spectator());
  }

  public static boolean ladder(World world, int x, int y, int z) {
    if (y <= 0 || !world.method_155(x, y - 1, z, x, y, z)
        || world.getBlockId(x, y, z) != Block.field_1913.id) return false;
    int trapdoor = world.method_1778(x, y, z);
    // Beta's four trapdoor walls 0..3 correspond to native ladder faces 2..5.
    return (trapdoor & 4) != 0 && world.getBlockId(x, y - 1, z) == Block.field_1854.id
        && world.method_1778(x, y - 1, z) == (trapdoor & 3) + 2;
  }

  public static boolean canFit(Entity entity, Box bounds, List<Box> collisions) {
    if (collisions.isEmpty()) return true;
    List<Box> adjusted = movementCollisions(entity, collisions);
    if (adjusted == collisions) return false;
    for (Box obstacle : adjusted) if (obstacle.intersects(bounds)) return false;
    return true;
  }

  public static List<Box> movementCollisions(Entity entity, List<Box> collisions) {
    if (!(entity instanceof PlayerEntity) || !enabled(entity) || collisions.isEmpty()) return collisions;
    List<Box> adjusted = null;
    for (int index = 0; index < collisions.size(); index++) {
      Box obstacle = collisions.get(index);
      double width = obstacle.maxX - obstacle.minX, depth = obstacle.maxZ - obstacle.minZ;
      if (!(width == .1875 && depth == 1 || width == 1 && depth == .1875)
          || obstacle.maxY - obstacle.minY != 1) continue;
      int x = MathHelper.floot(obstacle.minX), y = MathHelper.floot(obstacle.minY), z = MathHelper.floot(obstacle.minZ);
      if (obstacle.minY != y || obstacle.maxY != y + 1 || !ladder(entity.world, x, y, z)) continue;
      int facing = entity.world.method_1778(x, y, z) & 3;
      double minX = x, minZ = z, maxX = x + 1, maxZ = z + 1;
      switch (facing) {
        case 0 -> minZ += .8125;
        case 1 -> maxZ -= .8125;
        case 2 -> minX += .8125;
        case 3 -> maxX -= .8125;
      }
      // Match only the actual native leaf, never another block or entity collider.
      if (obstacle.minX != minX || obstacle.maxX != maxX || obstacle.minZ != minZ || obstacle.maxZ != maxZ) continue;
      // 26.3 ladders and open leaves share a 3/16 wall. Beta's ladder is 2/16.
      // Continue that native wall through the leaf for this movement query, so the
      // ordinary axis solver keeps both vertical passage and horizontal contact.
      switch (facing) {
        case 0 -> minZ += .0625;
        case 1 -> maxZ -= .0625;
        case 2 -> minX += .0625;
        case 3 -> maxX -= .0625;
      }
      if (adjusted == null) adjusted = new ArrayList<>(collisions);
      adjusted.set(index, Box.getOrCreate(minX, y, minZ, maxX, y + 1, maxZ));
    }
    return adjusted == null ? collisions : adjusted;
  }
}
