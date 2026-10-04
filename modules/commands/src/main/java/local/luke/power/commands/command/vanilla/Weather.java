package local.luke.power.commands.command.vanilla;

import java.util.List;
import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.*;

public final class Weather implements Command {
  public String name() {
    return "weather";
  }

  public void command(SharedCommandSource s, String[] args) {
    if (args.length < 2 || args.length > 3) {
      manual(s);
      return;
    }
    if (!List.of("clear", "rain", "thunder").contains(args[1]))
      throw new IllegalArgumentException("Choose clear, rain or thunder.");
    int ticks = args.length == 3 ? CommandNumbers.ticks(args[2], 20) : 6000;
    if (ticks < 1)
      throw new IllegalArgumentException("Weather duration must be at least one tick.");
    var info = EntityTargets.player(s).world.getProperties();
    info.setRaining(!args[1].equals("clear"));
    info.setThundering(args[1].equals("thunder"));
    info.setRainTime(ticks);
    info.setThunderTime(ticks);
    s.sendFeedback("§aWeather set to " + args[1] + " for " + ticks + " ticks.");
  }

  public void manual(SharedCommandSource s) {
    s.sendFeedback("/weather <clear|rain|thunder> [duration]");
    s.sendFeedback(
        "Duration defaults to 300 seconds. Add t for ticks, s for seconds or d for days.");
  }

  public String[] suggestion(SharedCommandSource s, int n, String input, String total) {
    return CommandSuggestions.suffix(
        input, n == 1 ? List.of("clear", "rain", "thunder") : List.of());
  }
}
