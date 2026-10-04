package local.luke.power.commands.command.vanilla;

import java.util.List;
import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

public final class Difficulty implements Command {
  private static final List<String> VALUES = List.of("peaceful", "easy", "normal", "hard");

  public String name() {
    return "difficulty";
  }

  public void command(SharedCommandSource s, String[] args) {
    var world = EntityTargets.player(s).world;
    if (args.length == 1) {
      s.sendFeedback(
          "§6Difficulty §8| §b" + VALUES.get(Math.max(0, Math.min(3, world.difficulty))));
      return;
    }
    if (args.length != 2) {
      manual(s);
      return;
    }
    int mode = VALUES.indexOf(args[1]);
    if (mode < 0) mode = CommandNumbers.integer(args[1], 0, 3, "Difficulty");
    if (s.isClient()) {
      Minecraft mc = (Minecraft) FabricLoader.getInstance().getGameInstance();
      mc.options.difficulty = mode;
      mc.options.save();
    }
    world.difficulty = mode;
    s.sendFeedback("§aDifficulty set to " + VALUES.get(mode) + ".");
  }

  public void manual(SharedCommandSource s) {
    s.sendFeedback("/difficulty [peaceful|easy|normal|hard]");
    s.sendFeedback(
        "No argument shows the current difficulty. Beta shares this setting between worlds;"
            + " peaceful removes hostile mobs.");
  }

  public String[] suggestion(SharedCommandSource s, int n, String input, String total) {
    return CommandSuggestions.suffix(input, n == 1 ? VALUES : List.of());
  }
}
