package local.luke.power.commands.util;

import local.luke.power.input.ReportedPosition;
import net.minecraft.entity.player.PlayerEntity;

/** Local coordinate reporting; other players and dedicated servers retain native origins. */
public final class CommandPosition {
  private CommandPosition() {}
  public static ReportedPosition.Pose of(PlayerEntity player) {
    return ReportedPosition.current(player, player.x, player.boundingBox.minY, player.z,
        player.yaw, player.pitch);
  }
}
