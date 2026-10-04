package local.luke.power.commands.command.vanilla;

import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.*;

public final class Seed implements Command {
  public String name() {
    return "seed";
  }

  public boolean needsPermissions() {
    return false;
  }

  public void command(SharedCommandSource s, String[] a) {
    if (a.length != 1) {
      manual(s);
      return;
    }
    s.sendFeedback("§6Seed §8| §b" + EntityTargets.player(s).world.getSeed());
  }

  public void manual(SharedCommandSource s) {
    s.sendFeedback("/seed | Show this world's generation seed.");
  }
}
