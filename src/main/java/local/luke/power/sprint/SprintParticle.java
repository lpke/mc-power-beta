package local.luke.power.sprint;

import java.util.ArrayList;
import local.luke.power.map.MapSampler;
import net.minecraft.block.Block;
import net.minecraft.class_563;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

/** Native terrain atlas/tints with modern dust gravity, dimensions and collision stopping. */
public final class SprintParticle extends class_563 {
  private boolean stopped;

  public SprintParticle(
      World world,
      double x,
      double y,
      double z,
      double vx,
      double vy,
      double vz,
      Block block,
      int metadata,
      int bx,
      int by,
      int bz) {
    super(world, x, y, z, vx, vy, vz, block, 0, metadata);
    field_2641 = 1;
    field_1631 = 0;
    method_1340(x, y, z);
    prevX = x;
    prevY = y;
    prevZ = z;
    double dx = vx + (Math.random() * 2 - 1) * .4f,
        dy = vy + (Math.random() * 2 - 1) * .4f,
        dz = vz + (Math.random() * 2 - 1) * .4f;
    double speed = (Math.random() + Math.random() + 1) * .15f,
        length = Math.sqrt(dx * dx + dy * dy + dz * dz);
    velocityX = dx / length * speed * .4f;
    velocityY = dy / length * speed * .4f + .1f;
    velocityZ = dz / length * speed * .4f;
    method_1856(bx, by, bz);
  }

  @Override
  public void tick() {
    prevX = x;
    prevY = y;
    prevZ = z;
    if (field_2638++ >= maxAge) {
      markDead();
      return;
    }
    velocityY -= .04;
    if (!stopped) drift();
    velocityX *= .98f;
    velocityY *= .98f;
    velocityZ *= .98f;
    if (field_1623) {
      velocityX *= .7f;
      velocityZ *= .7f;
    }
  }

  /** Particle collisions do not run entity movement callbacks or collide with boats/mobs. */
  private void drift() {
    double dx = velocityX, dy = velocityY, dz = velocityZ, oldY = dy;
    Box sweep = boundingBox.add(dx, dy, dz);
    var boxes = new ArrayList<Box>();
    int x0 = (int) Math.floor(sweep.minX),
        x1 = (int) Math.floor(sweep.maxX),
        z0 = (int) Math.floor(sweep.minZ),
        z1 = (int) Math.floor(sweep.maxZ);
    for (int bx = x0 - 1; bx <= x1 + 1; bx++)
      for (int bz = z0 - 1; bz <= z1 + 1; bz++)
        if (MapSampler.loaded(world, bx >> 4, bz >> 4) == null) return;
    for (int bx = x0; bx <= x1; bx++)
      for (int bz = z0; bz <= z1; bz++)
        for (int by = Math.max(0, (int) Math.floor(sweep.minY) - 1);
            by <= Math.min(127, (int) Math.floor(sweep.maxY));
            by++) {
          int id = MapSampler.loaded(world, bx >> 4, bz >> 4).method_859(bx & 15, by, bz & 15);
          if (id > 0 && id < Block.BLOCKS.length && Block.BLOCKS[id] != null)
            Block.BLOCKS[id].method_1562(world, bx, by, bz, sweep, boxes);
        }
    for (Box obstacle : boxes) dy = obstacle.getYOffset(boundingBox, dy);
    boundingBox.shift(0, dy, 0);
    boolean zFirst = Math.abs(dx) < Math.abs(dz);
    if (zFirst) {
      for (Box obstacle : boxes) dz = obstacle.getZOffset(boundingBox, dz);
      boundingBox.shift(0, 0, dz);
    }
    for (Box obstacle : boxes) dx = obstacle.getXOffset(boundingBox, dx);
    boundingBox.shift(dx, 0, 0);
    if (!zFirst) {
      for (Box obstacle : boxes) dz = obstacle.getZOffset(boundingBox, dz);
      boundingBox.shift(0, 0, dz);
    }
    x = (boundingBox.minX + boundingBox.maxX) / 2;
    y = boundingBox.minY;
    z = (boundingBox.minZ + boundingBox.maxZ) / 2;
    if (Math.abs(oldY) >= (double) 1e-5f && Math.abs(dy) < (double) 1e-5f) stopped = true;
    field_1623 = oldY != dy && oldY < 0;
    if (dx != velocityX) velocityX = 0;
    if (dz != velocityZ) velocityZ = 0;
  }
}
