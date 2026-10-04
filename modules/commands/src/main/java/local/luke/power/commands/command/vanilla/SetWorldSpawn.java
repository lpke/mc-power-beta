package local.luke.power.commands.command.vanilla;

import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.*;

public final class SetWorldSpawn implements Command {
  public String name() {
    return "setworldspawn";
  }

  public void command(SharedCommandSource s, String[] a) {
    if (a.length != 1 && a.length != 4) {
      manual(s);
      return;
    }
    var player = EntityTargets.player(s);
    var pos = SpawnPosition.parse(player, a, 1);
    player.world.setSpawnPos(pos);
    s.sendFeedback("§aWorld spawn set to " + pos.x + " " + pos.y + " " + pos.z + ".");
  }

  public void manual(SharedCommandSource s) {
    s.sendFeedback("/setworldspawn [x y z]");
    s.sendFeedback(
        "Set the Overworld default spawn. Personal spawns stay unchanged. Beta does not store a"
            + " world-spawn angle.");
  }

  public String[] suggestion(SharedCommandSource s, int n, String input, String total) {
    return CommandSuggestions.suffix(input, java.util.List.of("~", "^"));
  }
}
