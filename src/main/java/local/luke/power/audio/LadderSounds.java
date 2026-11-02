package local.luke.power.audio;

import local.luke.power.input.PlayerMode;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/** Modern ladder sounds reuse native actions and the shared modern movement stride. */
public final class LadderSounds {
  private LadderSounds() {}

  public static boolean enabled(World world) {
    return AudioConfig.current().ladderSounds && FluidSounds.localWorld(world);
  }

  public static boolean movement(Entity entity) {
    return entity instanceof net.minecraft.entity.LivingEntity && enabled(entity.world)
        && !entity.dead && entity.field_1595 == null && !entity.field_1642
        && !(entity instanceof PlayerMode mode && (mode.power$spectator() || mode.power$flying()))
        && !(entity.field_1623 && entity.method_1373());
  }

  public static boolean at(World world, double x, double y, double z) {
    return enabled(world) && FluidSounds.loaded(world, x, y, z)
        && world.getBlockId((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(z)) == Block.field_1854.id;
  }

  public static boolean step(Entity entity, int stepBlock) {
    return movement(entity) && stepBlock == Block.field_1854.id
        && !local.luke.power.gameplay.ModernSwimming.swimming(entity);
  }

  public static void step(Entity entity) {
    FluidSounds.entitySound(entity, "block.ladder.step", .15f, 1);
  }
}
