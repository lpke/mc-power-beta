package local.luke.power.commands.command.extra;

import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.*;

public class ToggleDownfall implements Command {
  public void command(SharedCommandSource s, String[] args) {
    if (args.length != 1) {
      manual(s);
      return;
    }
    var info = EntityTargets.player(s).world.getProperties();
    boolean rain = !info.getRaining();
    info.setRaining(rain);
    info.setThundering(false);
    info.setRainTime(6000);
    info.setThunderTime(6000);
    s.sendFeedback("§aWeather set to " + (rain ? "rain" : "clear") + ".");
  }

  public String name() {
    return "toggledownfall";
  }

  public void manual(SharedCommandSource s) {
    s.sendFeedback("/toggledownfall | Switch between rain and clear weather.");
    s.sendFeedback("Use /weather <clear|rain|thunder> [duration] for precise control.");
  }
}
