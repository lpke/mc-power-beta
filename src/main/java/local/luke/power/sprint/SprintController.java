package local.luke.power.sprint;

import local.luke.power.input.*;
import local.luke.power.rules.GameRule;
import local.luke.power.rules.WorldRules;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.lwjgl.opengl.Display;

public final class SprintController {
  private static float previousFov = 1, fov = 1;
  private static Object owner;

  private SprintController() {}

  private static Minecraft mc() {
    return (Minecraft) FabricLoader.getInstance().getGameInstance();
  }

  /** Shared by input ownership and movement. Multiplayer remains server-owned. */
  public static boolean allowed(Minecraft mc) {
    return mc.world != null
        && !mc.world.isRemote
        && WorldRules.enabled(mc.world.method_262(), GameRule.ALLOW_SPRINTING);
  }

  public static boolean active(Entity entity) {
    return entity instanceof ClientPlayerEntity
        && entity == mc().player
        && entity instanceof SprintPlayer p
        && p.power$sprint().sprinting
        && eligible((ClientPlayerEntity) entity);
  }

  private static boolean eligible(ClientPlayerEntity player) {
    var mc = mc();
    return allowed(mc)
        && player.world != null
        && !player.world.isRemote
        && player == mc.player
        && player.world == mc.world
        && player.health > 0
        && !player.dead
        && player.field_1595 == null
        && mc.currentScreen == null
        && !mc.paused
        && Display.isActive()
        && !DetachedCamera.isActive()
        && player instanceof PlayerMode mode
        && !mode.power$flying()
        && !mode.power$spectator()
        && !player.method_1334()
        && !player.method_1335()
        && !player.method_1373();
  }

  /** Runs after native input refresh and before the native jump/travel calls. */
  public static void movement(ClientPlayerEntity player) {
    SprintState state = ((SprintPlayer) player).power$sprint();
    var settings = SprintConfig.current();
    boolean eligible = eligible(player);
    boolean key = Bindings.down("powerbeta.sprint");
    if (player != owner) {
      owner = player;
      state.cancel(key);
    }
    state.tick(
        settings, eligible, player.field_161.field_2533, key, player.field_1623, player.field_1624,
        usesStamina(player));
    SprintView.publish(SprintView.Kind.GROUND, state.sprinting, settings.speed / 100);
  }

  public static float acceleration(Entity player, float value) {
    return active(player)
        ? SprintState.acceleration(value, player.field_1623, SprintConfig.current())
        : value;
  }

  public static void jumped(Entity entity) {
    if (!active(entity)) return;
    if (SprintConfig.current().jumpBoost) {
      entity.velocityX += SprintState.jumpX(entity.yaw);
      entity.velocityZ += SprintState.jumpZ(entity.yaw);
    }
    ((SprintPlayer) entity).power$sprint().jump(SprintConfig.current(), usesStamina((ClientPlayerEntity)entity));
  }

  public static boolean usesStamina(ClientPlayerEntity player) {
    return !DetachedCamera.isActive() && player instanceof PlayerMode mode && !mode.power$creative()
        && !mode.power$spectator() && !mode.power$flying();
  }

  public static void combat(Entity entity) {
    if (entity instanceof ClientPlayerEntity && entity == mc().player && !entity.world.isRemote)
      ((SprintPlayer) entity).power$sprint().combat(SprintConfig.current());
  }

  public static void particles(ClientPlayerEntity player) {
    if (!active(player)
        || !SprintConfig.current().particles
        || !player.field_1623
        || player.method_1334()
        || player.method_1335()) return;
    int x = (int) Math.floor(player.x),
        y = (int) Math.floor(player.boundingBox.minY - .2),
        z = (int) Math.floor(player.z);
    if (!player.world.method_239(x, y, z)) return;
    int id = player.world.getBlockId(x, y, z);
    if (id <= 0 || id >= Block.BLOCKS.length || Block.BLOCKS[id] == null) return;
    double px = player.x + (Math.random() - .5) * player.spacingXZ,
        pz = player.z + (Math.random() - .5) * player.spacingXZ;
    mc().field_2808
        .method_325(
            new SprintParticle(
                player.world,
                px,
                player.boundingBox.minY + .1,
                pz,
                -player.velocityX * 4,
                1.5,
                -player.velocityZ * 4,
                Block.BLOCKS[id],
                player.world.method_1778(x, y, z),
                x,
                y,
                z));
  }

  public static void tick(Minecraft mc) {
    SprintView.configure(SprintController::fov);
    rulesChanged(mc);
    previousFov = fov;
    var settings = SprintConfig.current();
    double ratio = 1;
    if (mc.player instanceof SprintPlayer player && (mc.world == null || !eligible(mc.player)))
      player.power$sprint().cancel(Bindings.down("powerbeta.sprint"));
    SprintView.publish(
        SprintView.Kind.GROUND,
        mc.player instanceof SprintPlayer p && p.power$sprint().sprinting,
        settings.speed / 100);
    if (mc.world != null && mc.player != null) {
      if (DetachedCamera.isActive()) {
        if (settings.freecamFov) ratio = SprintView.ratio(SprintView.Kind.FREECAM);
      } else if (mc.player instanceof PlayerMode mode && mode.power$flying()) {
        if (mode.power$spectator() && settings.dynamicFov)
          ratio = SprintView.ratio(SprintView.Kind.SPECTATOR);
        else if (!mode.power$spectator() && settings.dynamicFov)
          ratio = SprintView.ratio(SprintView.Kind.CREATIVE);
      } else if (settings.dynamicFov) ratio = SprintView.ratio(SprintView.Kind.GROUND);
    } else SprintView.publish(SprintView.Kind.GROUND, false, 1);
    double strength = DetachedCamera.isActive() ? 1 : settings.fovStrength / 100;
    double target = 1 + (ratio - 1) * .5 * strength;
    fov += (Math.max(1, Math.min(1.5, target)) - fov) * .5;
    if (Math.abs(fov - 1) < 1e-5) fov = 1;
  }

  /** Clear only ground sprint when its rule is revoked, including while a menu pauses ticks. */
  public static void rulesChanged(Minecraft mc) {
    if (allowed(mc)) return;
    if (mc.player instanceof SprintPlayer player)
      player.power$sprint().cancel(false);
    SprintView.publish(SprintView.Kind.GROUND, false, 1);
    if (!DetachedCamera.isActive()
        && !(mc.player instanceof PlayerMode mode && mode.power$flying()))
      previousFov = fov = 1;
  }

  public static double fov(double delta) {
    return previousFov + (fov - previousFov) * delta;
  }

  public static void reset() {
    owner = null;
    previousFov = fov = 1;
    SprintView.publish(SprintView.Kind.GROUND, false, 1);
  }
}
