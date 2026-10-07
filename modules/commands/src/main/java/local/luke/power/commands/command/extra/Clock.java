package local.luke.power.commands.command.extra;

import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.SharedCommandSource;
import net.minecraft.entity.player.PlayerEntity;

public class Clock implements Command {
  public void command(SharedCommandSource commandSource, String[] parameters) {
    if (parameters.length != 1) {
      manual(commandSource);
      return;
    }
    PlayerEntity player = commandSource.getPlayer();
    if (player == null) {
      return;
    }

    commandSource.sendFeedback("Time is " + String.valueOf(((local.luke.power.world.WorldCycles) player.world.getProperties()).power$daylightTime()));
    commandSource.sendFeedback("Days: " + String.valueOf((int) (((local.luke.power.world.WorldCycles) player.world.getProperties()).power$daylightTime() / 24000)));
  }

  @Override
  public String name() {
    return "clock";
  }

  @Override
  public void manual(SharedCommandSource commandSource) {
    commandSource.sendFeedback("Usage: /clock");
    commandSource.sendFeedback("Info: tells you the time in-game");
  }

  @Override
  public boolean needsPermissions() {
    return false;
  }
}
