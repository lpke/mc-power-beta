package local.luke.power.gameplay;

import local.luke.power.input.InteractionEye;
import local.luke.power.input.PlayerMode;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.minecraft.class_212;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.modificationstation.stationapi.api.event.entity.player.PlayerEvent;

/** Modern crouching retains its Survival block reach independently of camera lowering. */
public final class CrouchReach {
  public record PickContext(Object actor, float delta) {}
  private static final ThreadLocal<PickContext> PICK = new ThreadLocal<>();

  /** Native rendering queries reach before it casts its partial-tick ray. */
  public static PickContext beginPick(Object actor, float delta) {
    PickContext previous = PICK.get();
    PICK.set(new PickContext(actor, delta));
    return previous;
  }

  public static void endPick(PickContext previous) {
    if (previous == null) PICK.remove();
    else PICK.set(previous);
  }

  public static boolean active(Object actor) {
    var settings = GameplayConfig.current();
    return settings.modernCrawling && settings.crouchCameraHeight
        && actor instanceof ClientPlayerEntity player
        && player instanceof PlayerMode mode && !mode.power$creative()
        && ModernWater.localMovement(player) && ModernSwimming.crouched(player)
        && !ModernSwimming.compact(player);
  }

  @EventListener
  public void adjust(PlayerEvent.Reach event) {
    if (event.type != class_212.TILE || event.currentReach != 4.0 || !active(event.player)) return;
    var player = (ClientPlayerEntity) event.player;
    // Keep the actual camera ray. Its higher eye must still reach the same wall
    // below the feet when the optional camera lowering is disabled or easing.
    PickContext context = PICK.get();
    float delta = context != null && context.actor == player ? context.delta : 1;
    // Measure only the selected eye height. Interpolating world Y against the
    // current feet would turn ordinary vertical movement into extra reach.
    double eyeHeight = player.field_1631 - player.field_1640 + InteractionEye.offset(player, delta, 0);
    float pitch = player.prevPitch + (player.pitch - player.prevPitch) * delta;
    event.currentReach = CrouchReachGeometry.range(eyeHeight, pitch);
  }
}
