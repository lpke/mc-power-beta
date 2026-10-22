package local.luke.power.audio;

import java.util.Random;
import local.luke.power.input.PlayerMode;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/** Native movement/display events supply positions; these checks never scan or load terrain. */
public final class FluidSounds {
  private FluidSounds() {}
  public static boolean localWorld(World world) {
    if (world == null || world.isRemote) return false;
    Object game = FabricLoader.getInstance().getGameInstance();
    return game instanceof Minecraft mc && mc.world == world && mc.player != null && mc.player.world == world;
  }
  public static boolean localPlayer(Entity entity) {
    if (!(entity instanceof net.minecraft.entity.player.ClientPlayerEntity) || !localWorld(entity.world)) return false;
    var mc = (Minecraft)FabricLoader.getInstance().getGameInstance();
    return mc.player == entity && !entity.dead && mc.player.health > 0
        && !(entity instanceof PlayerMode mode && mode.power$spectator());
  }
  public static boolean movement(Entity entity) {
    return AudioConfig.current().swimmingSounds && localPlayer(entity) && entity.field_1595 == null
        && !entity.field_1642 && !(entity instanceof PlayerMode mode && mode.power$flying())
        && loaded(entity.world, entity.x, entity.boundingBox.minY, entity.z);
  }
  public static int stepBlock(Entity entity) {
    double y = entity.boundingBox.minY - 1e-5;
    return loaded(entity.world, entity.x, y, entity.z)
        ? entity.world.getBlockId((int)Math.floor(entity.x), (int)Math.floor(y), (int)Math.floor(entity.z)) : 0;
  }
  public static boolean swimStep(Entity entity, int stepBlock, double dy) {
    boolean swimming = local.luke.power.gameplay.ModernSwimming.swimming(entity);
    return stepBlock > 0 && entity.method_1334() && (swimming
        || !entity.field_1623 && stepBlock != net.minecraft.block.Block.field_1854.id
            && !(entity.method_1373() && dy == 0));
  }
  public static boolean loaded(World world, double x, double y, double z) {
    return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z) && y >= 0 && y < 128
        && world.method_239((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(z));
  }
  public static void swim(Entity entity, Random random) {
    if (!movement(entity) || !entity.method_1334()) return;
    float volume = FluidSoundMath.volume(entity.velocityX, entity.velocityY, entity.velocityZ, .35f);
    if (volume > 0) entity.world.method_191(entity, "power_beta:entity.player.swim", volume,
        1 + (random.nextFloat() - random.nextFloat()) * .4f);
  }
  public static void lavaPop(World world, double x, double y, double z, Random random) {
    if (!AudioConfig.current().lavaPopSounds || !localWorld(world) || !loaded(world, x, Math.max(0, y - 1), z)) return;
    world.method_150(x, y, z, "power_beta:block.lava.pop", .2f + random.nextFloat() * .2f, .9f + random.nextFloat() * .15f);
  }
}
