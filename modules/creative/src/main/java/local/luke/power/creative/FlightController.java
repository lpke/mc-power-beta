package local.luke.power.creative;

import local.luke.power.creative.api.ModePlayer;
import local.luke.power.creative.config.Config;
import local.luke.power.creative.flight.FlightPhysics;
import local.luke.power.creative.flight.FlightSprint;
import local.luke.power.input.MovementOwnership;
import local.luke.power.input.SprintView;
import local.luke.power.rules.GameRule;
import local.luke.power.rules.WorldRules;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.living.player.AbstractClientPlayer;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.level.Level;

public final class FlightController {
  private static int lastJump = -100, ticks;
  private static boolean jumpDown, sprinting;
  private static final FlightSprint sprint = new FlightSprint();
  private static PlayerEntity owner;
  private static Level ownerWorld;

  private FlightController() {}

  public static void reset() {
    lastJump = -100;
    jumpDown = true;
    sprinting = false;
    local.luke.power.input.SprintView.publish(local.luke.power.input.SprintView.Kind.CREATIVE, false, 1);
    local.luke.power.input.SprintView.publish(local.luke.power.input.SprintView.Kind.SPECTATOR, false, 1);
    sprint.reset(local.luke.power.input.Bindings.down(Keys.SPRINT));
  }

  /** Called before the native flight flag changes, while ground sprint is still published. */
  public static void flightChanged(PlayerEntity player, boolean flying) {
    Minecraft mc = ClientRuntime.minecraft();
    if (mc == null || owner != player || mc.player != player
        || ownerWorld != mc.level || player.level != mc.level) return;
    boolean pressed = local.luke.power.input.Bindings.down(Keys.SPRINT);
    boolean inherit = flying && ClientRuntime.active(mc) && !Modes.spectator(player)
        && Modes.canEnableFlight(player) && !player.isSleeping()
        && WorldRules.enabled(player.level.getProperties(), GameRule.ALLOW_SPRINTING)
        && SprintView.active(SprintView.Kind.GROUND);
    sprint.takeoff(inherit, pressed);
    sprinting = false;
    SprintView.publish(SprintView.Kind.CREATIVE, false, 1);
    SprintView.publish(SprintView.Kind.SPECTATOR, false, 1);
  }

  public static void tick(Minecraft mc) {
    if (owner != mc.player || ownerWorld != mc.level) {
      owner = mc.player;
      ownerWorld = mc.level;
      reset();
    }
    if (!ClientRuntime.moving(mc)) {
      sprinting = false;
      local.luke.power.input.SprintView.publish(local.luke.power.input.SprintView.Kind.CREATIVE, false, 1);
      local.luke.power.input.SprintView.publish(local.luke.power.input.SprintView.Kind.SPECTATOR, false, 1);
      sprint.reset(local.luke.power.input.Bindings.down(Keys.SPRINT));
      lastJump = -100;
      jumpDown = local.luke.power.input.Bindings.down(mc.options.jumpKey);
      return;
    }
    ticks++;
    boolean down = movementKey(mc, mc.options.jumpKey);
    if (Modes.canEnableFlight(mc.player) && down && !jumpDown) {
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
    sprinting =
        sprint.update(
            movementKey(mc, Keys.SPRINT),
            Modes.flying(mc.player)
                && ((movementKey(mc, mc.options.forwardKey) != movementKey(mc, mc.options.backKey))
                    || (movementKey(mc, mc.options.leftKey) != movementKey(mc, mc.options.rightKey))
                    || (movementKey(mc, mc.options.jumpKey) != movementKey(mc, mc.options.sneakKey))),
            Modes.spectator(mc.player) ? Config.current().spectatorSprintToggle : Config.current().sprintToggle);
    local.luke.power.input.SprintView.publish(local.luke.power.input.SprintView.Kind.CREATIVE,
        sprinting && !Modes.spectator(mc.player), Config.current().sprintMultiplier / 100.0);
    local.luke.power.input.SprintView.publish(local.luke.power.input.SprintView.Kind.SPECTATOR,
        sprinting && Modes.spectator(mc.player), Config.current().spectatorSprintMultiplier / 100.0);
  }

  private static boolean movementKey(Minecraft mc, Object key) {
    return mc.currentScreen == null ? local.luke.power.input.Bindings.down(key)
        : local.luke.power.input.MovementScreens.keyboardDown(key);
  }

  public static boolean travel(PlayerEntity entity) {
    Minecraft mc = ClientRuntime.minecraft();
    if (mc == null || entity != mc.player || !Modes.flying(entity)) return false;
    AbstractClientPlayer player = mc.player;
    var settings = Config.current();
    boolean spectator = Modes.spectator(player);
    boolean manual = ClientRuntime.moving(mc);
    float automatic = ClientRuntime.local(mc) && mc.viewEntity == mc.player
        ? MovementOwnership.autoWalkInput() : 0;
    if (!manual && automatic == 0) {
      player.velocityX = player.velocityY = player.velocityZ = 0;
      animate(player, 0);
      return true;
    }
    var input = player.playerKeypressManager;
    double side = manual ? input.perpendicularMovement : 0;
    double forward = manual ? input.parallelMovement : automatic;
    // Beta scales movement while crouching; flying descends without this slowdown.
    if (manual && input.sneak) {
      side /= .3;
      forward /= .3;
    }
    int vertical = manual ? (input.jump ? 1 : 0) - (input.sneak ? 1 : 0) : 0;
    double speed =
        spectator ? ((ModePlayer) player).power_spectatorSpeed() : .05 * settings.flightSpeed / 100;
    double sprint = sprinting ? (spectator ? settings.spectatorSprintMultiplier : settings.sprintMultiplier) / 100.0 : 1;
    var velocity =
        FlightPhysics.step(
            new FlightPhysics.Motion(player.velocityX, player.velocityY, player.velocityZ),
            side,
            forward,
            vertical,
            player.yaw,
            speed,
            sprint,
            spectator ? settings.spectatorAcceleration : settings.acceleration,
            spectator ? settings.spectatorGlide : settings.glide);
    player.velocityX = velocity.x();
    player.velocityY = velocity.y();
    player.velocityZ = velocity.z();
    double previousX = player.x, previousZ = player.z;
    player.move(player.velocityX, player.velocityY, player.velocityZ);
    // Native travel normally owns this animation update. Our flight replaces travel.
    float walking = (float) Math.min(1, Math.hypot(player.x - previousX, player.z - previousZ) * 4);
    animate(player, walking);
    // Use collision-adjusted velocities so hitting walls cannot accumulate speed.
    player.velocityX *= local.luke.power.input.FlightMotion.HORIZONTAL_DRAG;
    player.velocityY *= local.luke.power.input.FlightMotion.VERTICAL_DRAG;
    player.velocityZ *= local.luke.power.input.FlightMotion.HORIZONTAL_DRAG;
    if (!spectator && settings.landStopsFlight && player.onGround && vertical <= 0)
      player.creative_setFlying(false);
    return true;
  }

  private static void animate(PlayerEntity player, float walking) {
    // Keep both interpolation endpoints current even when detached input stops flight.
    player.prevLimbDistance = player.limbDistance;
    player.limbDistance += (walking - player.limbDistance) * .4f;
    player.invLimbDistance += player.limbDistance;
  }

  public static int scroll(Minecraft mc, int delta) {
    if (delta == 0 || !ClientRuntime.active(mc) || !Modes.spectator(mc.player)) return delta;
    if (Config.current().spectatorScroll) {
      ModePlayer p = (ModePlayer) mc.player;
      p.power_spectatorSpeed(
          FlightPhysics.scroll(
              p.power_spectatorSpeed(), delta, Config.current().spectatorScrollStep / 2000f));
      ClientRuntime.showSpeed();
    }
    return 0;
  }
}
