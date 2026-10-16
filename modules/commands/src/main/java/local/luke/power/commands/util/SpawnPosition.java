package local.luke.power.commands.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3i;

public final class SpawnPosition {
  private SpawnPosition() {}

  public static Vec3i parse(PlayerEntity self, String[] args, int start) {
    if (self.dimensionId != 0)
      throw new IllegalArgumentException("Beta respawns in the Overworld. Set the spawn there.");
    var pose = CommandPosition.of(self);
    double[] p =
        args.length == start
            ? new double[] {pose.x(), pose.y(), pose.z()}
            : CommandNumbers.position(
                pose.x(), pose.y(), pose.z(), pose.yaw(), pose.pitch(), args, start);
    if (p[1] < 0 || p[1] > 126)
      throw new IllegalArgumentException("Spawn height must be from 0 to 126 in Beta.");
    return new Vec3i((int) Math.floor(p[0]), (int) Math.floor(p[1]), (int) Math.floor(p[2]));
  }

  public static float angle(String[] args, int index, PlayerEntity self) {
    if (args.length <= index) return 0;
    double angle = CommandNumbers.relative(args[index], CommandPosition.of(self).yaw()) % 360;
    if (!Double.isFinite(angle)) throw new IllegalArgumentException("Invalid spawn angle.");
    return (float) angle;
  }
}
