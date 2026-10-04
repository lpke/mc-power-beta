package local.luke.power.commands.command.vanilla;

import java.util.*;
import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.*;

public class Time implements Command {
  private static final Map<String, Integer> PRESETS =
      Map.of(
          "day",
          1000,
          "noon",
          6000,
          "night",
          13000,
          "midnight",
          18000,
          "sunset",
          12000,
          "sunrise",
          23000);

  public void command(SharedCommandSource source, String[] args) {
    var world = EntityTargets.player(source).world;
    if (args.length == 2 && args[1].equals("get")) args = new String[] {"time", "query", "daytime"};
    if (args.length != 3) {
      manual(source);
      return;
    }
    switch (args[1]) {
      case "set", "add" -> {
        long ticks =
            args[1].equals("set") && PRESETS.containsKey(args[2])
                ? PRESETS.get(args[2])
                : CommandNumbers.ticks(args[2], 1);
        long result = args[1].equals("add") ? Math.addExact(world.getTime(), ticks) : ticks;
        world.setTime(result);
        source.sendFeedback("§aTime set to " + result + ".");
      }
      case "query" -> {
        long time = world.getTime();
        long result =
            switch (args[2]) {
              case "daytime" -> Math.floorMod(time, 24000);
              case "day" -> Math.floorDiv(time, 24000);
              case "gametime" -> time;
              default -> throw new IllegalArgumentException("Choose daytime, gametime or day.");
            };
        source.sendFeedback("§b" + args[2] + "§7: " + result);
      }
      default -> manual(source);
    }
  }

  public String name() {
    return "time";
  }

  public void manual(SharedCommandSource s) {
    s.sendFeedback("/time set <day|noon|night|midnight|time> | /time add <time>");
    s.sendFeedback("/time query <daytime|gametime|day>");
    s.sendFeedback(
        "Durations accept t ticks, s seconds and d days. Beta uses one clock for world time and"
            + " game time.");
  }

  public String[] suggestion(SharedCommandSource s, int n, String input, String total) {
    return CommandSuggestions.suffix(
        input,
        n == 1
            ? List.of("set", "add", "query")
            : n == 2
                ? total.contains("query") ? List.of("daytime", "gametime", "day") : PRESETS.keySet()
                : List.of());
  }
}
