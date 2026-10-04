package local.luke.power.commands.command.extra;

import java.util.*;
import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.*;
import net.minecraft.entity.Entity;

public class Ride implements Command {
  public void command(SharedCommandSource s, String[] args) {
    if (args.length < 3 || args.length > 4) {
      manual(s);
      return;
    }
    Entity rider = EntityTargets.one(s, args[1]);
    if (args[2].equals("dismount") && args.length == 3) {
      rider.setVehicle(null);
      s.sendFeedback("§aDismounted " + EntityTargets.name(rider) + ".");
      return;
    }
    if (!args[2].equals("mount") || args.length != 4) {
      manual(s);
      return;
    }
    Entity mount = EntityTargets.one(s, args[3]);
    if (mount == rider) throw new IllegalArgumentException("An entity cannot ride itself.");
    if (rider.vehicle != null) throw new IllegalArgumentException("Dismount first.");
    if (mount.passenger != null)
      throw new IllegalArgumentException("The vehicle already has a passenger.");
    Set<Entity> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    for (Entity e = mount; e != null; e = e.vehicle)
      if (e == rider || !seen.add(e))
        throw new IllegalArgumentException("This would create a riding loop.");
    rider.setVehicle(mount);
    s.sendFeedback(
        "§aMounted " + EntityTargets.name(rider) + " on " + EntityTargets.name(mount) + ".");
  }

  public String name() {
    return "ride";
  }

  public void manual(SharedCommandSource s) {
    s.sendFeedback("/ride <target> mount <vehicle> | /ride <target> dismount");
    s.sendFeedback(
        "Each selector must match one loaded entity. Existing passengers are never replaced.");
  }

  public String[] suggestion(SharedCommandSource s, int n, String input, String total) {
    return n == 2
        ? CommandSuggestions.suffix(input, List.of("mount", "dismount"))
        : CommandSuggestions.targets(s, input);
  }
}
