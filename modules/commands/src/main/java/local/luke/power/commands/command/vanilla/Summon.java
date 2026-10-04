package local.luke.power.commands.command.vanilla;

import java.util.*;
import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityRegistry;

public class Summon implements Command {
  // Registry retained for existing integrations; modern syntax is the public entry point.
  public static Map<Class<? extends Entity>, String> help = new HashMap<>();

  public void command(SharedCommandSource s, String[] args) {
    if (args.length != 2 && args.length != 5) {
      manual(s);
      return;
    }
    var player = EntityTargets.player(s);
    String id = EntityTargets.registryName(args[1]);
    double[] p =
        args.length == 2
            ? new double[] {player.x, player.boundingBox.minY, player.z}
            : CommandNumbers.position(
                player.x, player.boundingBox.minY, player.z, player.yaw, player.pitch, args, 2);
    Entity entity = EntityRegistry.create(id, player.world);
    if (entity == null) throw new IllegalArgumentException("Cannot create this entity.");
    // Entity types whose constructor requires payloads need dedicated game actions.
    if (Set.of("Item", "Painting", "FallingSand", "FishingHook").contains(id))
      throw new IllegalArgumentException(
          "This Beta entity requires item or block data and cannot be summoned safely.");
    entity.setPositionAndAngles(
        p[0], p[1] + entity.standingEyeHeight, p[2], entity.yaw, entity.pitch);
    if (!player.world.spawnEntity(entity))
      throw new IllegalArgumentException("Entity could not be spawned here.");
    s.sendFeedback("§aSummoned " + EntityTargets.modern(id) + ".");
  }

  public String name() {
    return "summon";
  }

  public void manual(SharedCommandSource s) {
    s.sendFeedback("/summon <entity> [x y z]");
    s.sendFeedback(
        "Beta entity names and minecraft: names work. Coordinates support ~ and ^. Modern entity"
            + " NBT is unavailable in Beta.");
  }

  public String[] suggestion(SharedCommandSource s, int n, String input, String total) {
    if (n == 1) {
      List<String> names = new ArrayList<>();
      for (Object id : EntityRegistry.idToClass.keySet())
        names.add("minecraft:" + EntityTargets.modern(id.toString()));
      return CommandSuggestions.suffix(input, names);
    }
    return CommandSuggestions.suffix(input, n > 1 && n < 5 ? List.of("~", "^") : List.of());
  }
}
