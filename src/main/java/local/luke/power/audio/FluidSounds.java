package local.luke.power.audio;

import java.util.Random;
import local.luke.power.input.PlayerMode;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/** Audio samples bounded, loaded terrain without changing native fluid state. */
public final class FluidSounds {
  private static Random displayRandom = new Random();
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
  public static boolean splashOwner(Entity entity) {
    return localWorld(entity.world) && !(entity instanceof PlayerMode mode && mode.power$spectator());
  }
  public static String entityCategory(Entity entity) {
    if (entity instanceof net.minecraft.entity.player.PlayerEntity) return "players";
    if (entity instanceof net.minecraft.class_142) return "ambient";
    return entity instanceof net.minecraft.class_65 ? "hostile" : "passive";
  }
  public static void entitySound(Entity entity, String event, float volume, float pitch) {
    SoundCategoryScope.run(entityCategory(entity), () -> entity.world.method_150(entity.x,
        entity.boundingBox.minY, entity.z, "power_beta:" + event, volume, pitch));
  }
  public static void splash(Entity entity, Random random) {
    Entity rider = entity.field_1594;
    // Only boats have a controlling passenger among Beta vehicles.
    boolean controlled = rider instanceof net.minecraft.entity.player.PlayerEntity && entity instanceof net.minecraft.class_113;
    Entity motion = controlled ? rider : entity;
    float gain = FluidSoundMath.volume(motion.velocityX, motion.velocityY, motion.velocityZ, controlled ? .9f : .2f);
    String event = entity instanceof net.minecraft.entity.player.PlayerEntity
        ? FluidSoundMath.splash(gain).substring("power_beta:".length())
        : entity instanceof net.minecraft.class_65 && gain < .25f ? "entity.hostile.splash" : "entity.generic.splash";
    entitySound(entity, event, gain, 1 + (random.nextFloat() - random.nextFloat()) * .4f);
  }
  public static boolean movement(Entity entity) {
    return AudioConfig.current().swimmingSounds && localPlayer(entity) && entity.field_1595 == null
        && !entity.field_1642 && !(entity instanceof PlayerMode mode && mode.power$flying())
        && !(entity.field_1623 && entity.method_1373())
        && loaded(entity.world, entity.x, entity.boundingBox.minY, entity.z);
  }
  public static int stepBlock(Entity entity) {
    double y = entity.boundingBox.minY - 1e-5;
    return loaded(entity.world, entity.x, y, entity.z)
        ? entity.world.getBlockId((int)Math.floor(entity.x), (int)Math.floor(y), (int)Math.floor(entity.z)) : 0;
  }
  public static boolean swimStep(Entity entity, int stepBlock, double dy) {
    boolean swimming = local.luke.power.gameplay.ModernSwimming.swimming(entity);
    return stepBlock > 0 && inWater(entity) && (swimming
        || !entity.field_1623 && stepBlock != net.minecraft.block.Block.field_1854.id
            && !(entity.method_1373() && dy == 0));
  }
  public static boolean inWater(Entity entity) {
    return entity instanceof FluidSoundContact contact ? contact.power$audioInWater() : entity.method_1334();
  }
  /** Modern's 0.001-inset body check, bounded to loaded entity boxes. Never changes native fluids. */
  public static boolean bodyInWater(Entity entity) {
    var box = entity.boundingBox;
    if (!Double.isFinite(box.minX) || !Double.isFinite(box.minY) || !Double.isFinite(box.minZ)
        || !Double.isFinite(box.maxX) || !Double.isFinite(box.maxY) || !Double.isFinite(box.maxZ)) return false;
    double bottom = box.minY + .001;
    if (entity.field_1595 instanceof net.minecraft.class_113 boat && !boatUnderwater(boat))
      bottom = Math.max(bottom, boat.boundingBox.maxY);
    return bodyInWater(entity, bottom);
  }
  private static boolean boatUnderwater(Entity boat) {
    var box = boat.boundingBox; var world = boat.world;
    if (!Double.isFinite(box.minX) || !Double.isFinite(box.maxX) || !Double.isFinite(box.maxY)
        || !Double.isFinite(box.minZ) || !Double.isFinite(box.maxZ)) return false;
    double top = box.maxY + .001;
    int x0 = (int)Math.floor(box.minX), x1 = (int)Math.ceil(box.maxX);
    int z0 = (int)Math.floor(box.minZ), z1 = (int)Math.ceil(box.maxZ);
    int y0 = (int)Math.floor(box.maxY), y1 = (int)Math.ceil(top);
    if (x1 <= x0 || z1 <= z0 || y1 <= y0 || y0 < 0 || y1 > 128
        || x1 - x0 > 16 || z1 - z0 > 16) return false;
    if (!world.method_155(x0, y0, z0, x1 - 1, y1 - 1, z1 - 1)) return false;
    for (int x = x0; x < x1; x++) for (int y = y0; y < y1; y++) for (int z = z0; z < z1; z++)
      if (world.method_1779(x, y, z) == net.minecraft.block.Material.WATER && top < waterSurface(world, x, y, z)) return true;
    return false;
  }
  private static double waterSurface(World world, int x, int y, int z) {
    return y < 127 && world.method_1779(x, y + 1, z) == net.minecraft.block.Material.WATER ? y + 1
        : y + 1 - net.minecraft.class_385.method_1218(world.method_1778(x, y, z));
  }
  private static boolean bodyInWater(Entity entity, double bottom) {
    var box = entity.boundingBox; var world = entity.world;
    if (bottom >= box.maxY - .001) return false;
    int x0 = (int)Math.floor(box.minX + .001), x1 = (int)Math.ceil(box.maxX - .001);
    int y0 = Math.max(0, (int)Math.floor(bottom)), y1 = Math.min(128, (int)Math.ceil(box.maxY - .001));
    int z0 = (int)Math.floor(box.minZ + .001), z1 = (int)Math.ceil(box.maxZ - .001);
    if (x1 <= x0 || y1 <= y0 || z1 <= z0 || x1 - x0 > 16 || y1 - y0 > 16 || z1 - z0 > 16) return false;
    if (!world.method_155(x0 - 1, y0, z0 - 1, x1, y1 - 1, z1)) return false;
    for (int x = x0; x < x1; x++) for (int y = y0; y < y1; y++) for (int z = z0; z < z1; z++) {
      if (world.method_1779(x, y, z) != net.minecraft.block.Material.WATER) continue;
      double surface = waterSurface(world, x, y, z);
      if (surface >= bottom) return true;
    }
    return false;
  }
  public static boolean loaded(World world, double x, double y, double z) {
    return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z) && y >= 0 && y < 128
        && world.method_239((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(z));
  }
  public static void swim(Entity entity, Random random) {
    if (!movement(entity) || !inWater(entity)) return;
    float volume = FluidSoundMath.volume(entity.velocityX, entity.velocityY, entity.velocityZ, .35f);
    entity.world.method_150(entity.x, entity.boundingBox.minY, entity.z, "power_beta:entity.player.swim", volume,
        1 + (random.nextFloat() - random.nextFloat()) * .4f);
  }
  public static void lavaAmbient(World world, int x, int y, int z) {
    if (!AudioConfig.current().lavaAmbientSounds || !localWorld(world) || !loaded(world, x, y, z)
        || world.method_1779(x, y, z) != net.minecraft.block.Material.LAVA
        || world.method_1779(x, y + 1, z) != net.minecraft.block.Material.AIR || world.method_1783(x, y + 1, z)) return;
    if (displayRandom.nextInt(200) == 0) world.method_150(x, y, z, "power_beta:block.lava.ambient",
        .2f + displayRandom.nextFloat() * .2f, .9f + displayRandom.nextFloat() * .15f);
  }
  public static void lavaPop(World world, double x, double y, double z) {
    if (!AudioConfig.current().lavaPopSounds || !localWorld(world) || !loaded(world, x, Math.max(0, y - 1), z)) return;
    world.method_150(x, y, z, "power_beta:block.lava.pop", .2f + displayRandom.nextFloat() * .2f, .9f + displayRandom.nextFloat() * .15f);
  }
}
