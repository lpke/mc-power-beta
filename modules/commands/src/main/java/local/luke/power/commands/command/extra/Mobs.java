package local.luke.power.commands.command.extra;

import java.util.*;
import local.luke.power.chat.HelpOutput;
import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.*;
import net.minecraft.entity.EntityRegistry;

public class Mobs implements Command {
  public static Map<String, Class> getMobSet() {
    return EntityRegistry.idToClass;
  }

  public void command(SharedCommandSource s, String[] args) {
    if (args.length > 2) {
      manual(s);
      return;
    }
    List<String> lines = new ArrayList<>();
    for (String id : getMobSet().keySet())
      if (!Set.of("Item", "Painting", "FallingSand", "FishingHook").contains(id))
        lines.add("minecraft:" + EntityTargets.modern(id));
    lines.sort(String::compareTo);
    int page =
        args.length == 2 ? CommandNumbers.integer(args[1], 1, (lines.size() + 5) / 6, "Page") : 1;
    HelpOutput.print(
        s::sendFeedback,
        "Beta entities",
        lines,
        page,
        6,
        args.length == 1 && HelpOutput.scrolling());
  }

  public String name() {
    return "mobs";
  }

  public void manual(SharedCommandSource s) {
    s.sendFeedback("/mobs [page] | List entity names accepted by /summon.");
  }

  public boolean needsPermissions() {
    return false;
  }
}
