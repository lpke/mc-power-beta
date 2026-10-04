package local.luke.power.commands.command.extra;

import java.util.HashMap;
import java.util.Map;
import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.SharedCommandSource;
import net.minecraft.entity.player.PlayerEntity;

public class God implements Command {

  public static Map<String, Boolean> isPlayerInvincible = new HashMap<>();

  @Override
  public void command(SharedCommandSource commandSource, String[] parameters) {
    if (parameters.length != 1) {
      manual(commandSource);
      return;
    }
    boolean god = false;

    PlayerEntity player = commandSource.getPlayer();
    if (player == null) {
      return;
    }

    if (isPlayerInvincible.get(player.name) != null) {
      god = !isPlayerInvincible.get(player.name);
      isPlayerInvincible.replace(player.name, god);
    } else {
      isPlayerInvincible.put(player.name, true);
      god = true;
    }

    commandSource.sendFeedback("God mode " + (god ? "activated" : "deactivated"));
  }

  @Override
  public String name() {
    return "god";
  }

  @Override
  public void manual(SharedCommandSource commandSource) {
    commandSource.sendFeedback("Usage: /god");
    commandSource.sendFeedback("Info: Activates or deactivates invincibility");
  }
}
