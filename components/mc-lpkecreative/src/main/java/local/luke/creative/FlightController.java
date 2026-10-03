package local.luke.creative;

import local.luke.creative.api.ModePlayer;
import local.luke.creative.config.Config;
import local.luke.creative.flight.FlightPhysics;
import local.luke.creative.flight.FlightSprint;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.living.player.AbstractClientPlayer;
import net.minecraft.entity.living.player.PlayerEntity;

public final class FlightController {
  private static int lastJump = -100, ticks;
  private static boolean jumpDown, sprinting;
  private static final FlightSprint sprint = new FlightSprint();
  private static PlayerEntity owner;

  private FlightController() {}

  public static void reset() {
    lastJump = -100;
    jumpDown = true;
    sprinting = false;
    sprint.reset(local.luke.power.input.Bindings.down(Keys.SPRINT));
  }

  public static void tick(Minecraft mc) {
    if (owner != mc.player) {
      owner = mc.player;
      reset();
    }
    if (!ClientRuntime.active(mc)) {
      sprinting = false;
      sprint.reset(local.luke.power.input.Bindings.down(Keys.SPRINT));
      lastJump = -100;
      jumpDown = local.luke.power.input.Bindings.down(mc.options.jumpKey);
      return;
    }
    ticks++;
    boolean down = local.luke.power.input.Bindings.down(mc.options.jumpKey);
    if (mc.player.creative_isCreative() && Config.current().flight && down && !jumpDown) {
      if (ticks - lastJump <= Config.current().doubleTapTicks) {
        boolean fly = !mc.player.creative_isFlying();
        mc.player.creative_setFlying(fly);
        if (fly && mc.player.onGround) {
          mc.player.onGround = false;
          mc.player.velocityY = .42;
        }
        lastJump = -100;
      } else lastJump = ticks;
    }
    jumpDown = down;
    if (mc.player.creative_isCreative() && !Config.current().flight)
      mc.player.creative_setFlying(false);
    sprinting =
        sprint.update(
            local.luke.power.input.Bindings.down(Keys.SPRINT),
            Modes.flying(mc.player)
                && Config.current().sprintFlight
                && local.luke.power.input.Bindings.down(mc.options.forwardKey)
                && !local.luke.power.input.Bindings.down(mc.options.backKey),
            Config.current().sprintToggle);
  }

  public static boolean travel(PlayerEntity entity) {
    Minecraft mc = ClientRuntime.minecraft();
    if (mc == null || entity != mc.player || !Modes.flying(entity)) return false;
    AbstractClientPlayer player = mc.player;
    var settings = Config.current();
    boolean spectator = Modes.spectator(player);
    if (!ClientRuntime.active(mc)) {
      player.velocityX = player.velocityY = player.velocityZ = 0;
      return true;
    }
    var input = player.playerKeypressManager;
    double side = input.perpendicularMovement, forward = input.parallelMovement;
    // Beta scales movement while crouching; flying descends without this slowdown.
    if (input.sneak) {
      side /= .3;
      forward /= .3;
    }
    int vertical = (input.jump ? 1 : 0) - (input.sneak ? 1 : 0);
    double speed =
        spectator ? ((ModePlayer) player).lpke_spectatorSpeed() : .05 * settings.flightSpeed / 100;
    double sprint = settings.sprintFlight && sprinting ? settings.sprintMultiplier / 100.0 : 1;
    var velocity =
        FlightPhysics.step(
            new FlightPhysics.Motion(player.velocityX, player.velocityY, player.velocityZ),
            side,
            forward,
            vertical,
            player.yaw,
            speed,
            sprint,
            spectator ? 5 : settings.glide);
    player.velocityX = velocity.x();
    player.velocityY = velocity.y();
    player.velocityZ = velocity.z();
    double previousX = player.x, previousZ = player.z;
    player.move(player.velocityX, player.velocityY, player.velocityZ);
    // Native travel normally owns this animation update. Our flight replaces travel.
    player.prevLimbDistance = player.limbDistance;
    float walking = (float) Math.min(1, Math.hypot(player.x - previousX, player.z - previousZ) * 4);
    player.limbDistance += (walking - player.limbDistance) * .4f;
    player.invLimbDistance += player.limbDistance;
    // Use collision-adjusted velocities so hitting walls cannot accumulate speed.
    player.velocityX *= .91;
    player.velocityY *= .6;
    player.velocityZ *= .91;
    if (!spectator && settings.landStopsFlight && player.onGround && vertical <= 0)
      player.creative_setFlying(false);
    return true;
  }

  public static int scroll(Minecraft mc, int delta) {
    if (delta == 0 || !ClientRuntime.active(mc) || !Modes.spectator(mc.player)) return delta;
    if (Config.current().spectatorScroll) {
      ModePlayer p = (ModePlayer) mc.player;
      p.lpke_spectatorSpeed(
          FlightPhysics.scroll(
              p.lpke_spectatorSpeed(), delta, Config.current().spectatorScrollStep / 2000f));
      ClientRuntime.showSpeed();
    }
    return 0;
  }
}
