package local.luke.power.audio;

import local.luke.power.input.PlayerMode;
import local.luke.power.sprint.SprintController;
import net.minecraft.class_113;
import net.minecraft.class_549;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/** Successful local player attacks are the modern vehicle sound source. Vehicles have no break cue. */
public final class VehicleAttackSounds {
  private VehicleAttackSounds() {}

  public static boolean eligible(PlayerEntity player, Entity target) {
    return AudioConfig.current().vehicleAttackSounds && player != null && !player.world.isRemote
        && target != null && target.world == player.world && !target.dead
        && (target instanceof class_113 || target instanceof class_549)
        && !(player instanceof PlayerMode mode && mode.power$spectator());
  }

  public static void reset(PlayerEntity player) {
    if (AudioConfig.current().vehicleAttackSounds && player != null && !player.world.isRemote
        && !(player instanceof PlayerMode mode && mode.power$spectator()))
      ((VehicleAttackSoundPlayer) player).power$resetVehicleSoundCharge();
  }

  public static void hit(PlayerEntity player) {
    int ticks = ((VehicleAttackSoundPlayer) player).power$vehicleSoundCharge();
    var held = player.method_502(); int item = held == null ? 0 : held.itemId;
    boolean sprinting = SprintController.active(player);
    if (sprinting && VehicleAttackSoundMath.charged(ticks, item))
      ExtraSounds.play(player.world, player.x, player.boundingBox.minY, player.z, "entity.player.attack.knockback");
    // ServerPlayer.getKnownMovement uses accepted position displacement, before friction.
    double dx = player.x - player.prevX, dz = player.z - player.prevZ;
    ExtraSounds.play(player.world, player.x, player.boundingBox.minY, player.z,
        VehicleAttackSoundMath.hit(ticks, item, sprinting, player.field_1623,
            dx * dx + dz * dz));
  }
}
