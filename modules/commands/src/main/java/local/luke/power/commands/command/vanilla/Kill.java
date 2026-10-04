package local.luke.power.commands.command.vanilla;

import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.*;
import net.minecraft.entity.LivingEntity;

public class Kill implements Command {
  public void command(SharedCommandSource s, String[] args) {
    if (args.length > 2) {
      manual(s);
      return;
    }
    var targets = EntityTargets.resolve(s, args.length == 2 ? args[1] : "@s", false);
    for (var target : targets) {
      if (target instanceof LivingEntity living) {
        if (living.health > 0) {
          living.health = 0;
          living.onKilledBy(null);
        }
      } else target.markDead();
    }
    s.sendFeedback("§aKilled " + targets.size() + " entities.");
  }

  public String name() {
    return "kill";
  }

  public void manual(SharedCommandSource s) {
    s.sendFeedback("/kill [targets]");
    s.sendFeedback(
        "Defaults to yourself. @e[type=item,distance=..10] targets nearby dropped items. Death"
            + " cannot be undone.");
  }

  public String[] suggestion(SharedCommandSource s, int n, String input, String total) {
    return n == 1 ? CommandSuggestions.targets(s, input) : new String[0];
  }
}
