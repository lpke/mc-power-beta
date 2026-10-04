package local.luke.power.commands.command.vanilla;

import local.luke.power.commands.api.*;
import local.luke.power.commands.util.*;
import net.minecraft.entity.player.PlayerEntity;

public final class SpawnPoint implements Command {
  public String name() {
    return "spawnpoint";
  }

  public void command(SharedCommandSource s, String[] a) {
    if (a.length != 1 && a.length != 2 && a.length != 5 && a.length != 6) {
      manual(s);
      return;
    }
    var self = EntityTargets.player(s);
    var targets = EntityTargets.resolve(s, a.length == 1 ? "@s" : a[1], true);
    var pos = SpawnPosition.parse(self, a, a.length == 1 ? 1 : 2);
    float angle = SpawnPosition.angle(a, 5, self);
    for (var target : targets) {
      ((PlayerEntity) target).setSpawnPos(pos);
      ((ForcedSpawn) target).power$forcedSpawn(true, angle);
    }
    s.sendFeedback(
        "§aRespawn point set to "
            + pos.x
            + " "
            + pos.y
            + " "
            + pos.z
            + " for "
            + targets.size()
            + " player(s).");
  }

  public void manual(SharedCommandSource s) {
    s.sendFeedback("/spawnpoint [targets] [x y z] [angle]");
    s.sendFeedback(
        "Set an Overworld respawn point without a bed. Sleeping in a bed replaces it. Inventory and"
            + " warps stay unchanged.");
  }

  public String[] suggestion(SharedCommandSource s, int n, String input, String total) {
    return n == 1
        ? CommandSuggestions.targets(s, input)
        : CommandSuggestions.suffix(input, java.util.List.of("~", "^"));
  }
}
