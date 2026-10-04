package local.luke.power.commands.command.extra;

import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.SharedCommandSource;
import net.minecraft.entity.player.PlayerEntity;

public class Heal implements Command {
  @Override
  public void command(SharedCommandSource commandSource, String[] parameters) {

    PlayerEntity player = commandSource.getPlayer();
    if (player == null) {
      return;
    }

    if (parameters.length > 2) {
      manual(commandSource);
      return;
    }
    int amount =
        parameters.length == 2
            ? local.luke.power.commands.util.CommandNumbers.integer(parameters[1], 0, 20, "Health")
            : 20;
    player.health = Math.min(20, Math.max(0, player.health) + amount);
    commandSource.sendFeedback("§aHealth: " + player.health + "/20.");
  }

  @Override
  public String name() {
    return "heal";
  }

  @Override
  public void manual(SharedCommandSource commandSource) {
    commandSource.sendFeedback("/heal [amount=20]");
    commandSource.sendFeedback("Restore up to 20 health points. One heart is two points.");
  }
}
