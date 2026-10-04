package local.luke.power.commands.command.vanilla;

import java.util.*;
import local.luke.power.commands.api.Command;
import local.luke.power.commands.optionaldep.stapi.SwitchDimension;
import local.luke.power.commands.util.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class Teleport implements Command {
  /** Legacy callers use Beta entity Y. Modern command parsing below uses feet Y. */
  public static void teleport(PlayerEntity player, double x, double y, double z) {
    if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
      throw new IllegalArgumentException("Invalid destination.");
    if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER)
      ServerUtil.serverTeleport(player, x, y, z);
    else {
      player.setPositionAndAngles(x, y, z, player.yaw, player.pitch);
      player.velocityX = player.velocityY = player.velocityZ = 0;
      player.fallDistance = 0;
    }
  }

  public static boolean switchDimensions(SharedCommandSource source, String id) {
    if (!FabricLoader.getInstance().isModLoaded("station-dimensions-v0")) return false;
    SwitchDimension.go(source, id);
    return true;
  }

  public void command(SharedCommandSource source, String[] args) {
    PlayerEntity self = EntityTargets.player(source);
    if (args.length < 2) {
      manual(source);
      return;
    }
    int start = 1;
    List<Entity> targets = List.of(self);
    // Numeric player names/entity IDs are ambiguous with coordinates. A complete coordinate
    // form wins; otherwise parse the first argument as a target before accepting any mutation.
    boolean positionForm =
        args.length >= 4
            && CommandNumbers.coordinate(args[1])
            && CommandNumbers.coordinate(args[2])
            && CommandNumbers.coordinate(args[3])
            && (args.length == 4
                || args.length == 6
                || args.length > 4 && args[4].equals("facing"));
    if (args.length > 2 && !positionForm) {
      targets = EntityTargets.resolve(source, args[1], false);
      start = 2;
    }
    if (start == args.length - 1) {
      Entity destination = EntityTargets.one(source, args[start]);
      double x = destination.x, y = destination.boundingBox.minY, z = destination.z;
      float yaw = destination.yaw, pitch = destination.pitch;
      for (Entity target : targets) move(target, x, y, z, yaw, pitch);
    } else {
      double[] pos =
          CommandNumbers.position(
              self.x, self.boundingBox.minY, self.z, self.yaw, self.pitch, args, start);
      int rest = start + 3;
      Float yaw = null, pitch = null;
      if (args.length == rest + 2) {
        yaw = (float) CommandNumbers.relative(args[rest], self.yaw);
        pitch = (float) CommandNumbers.relative(args[rest + 1], self.pitch);
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch))
          throw new IllegalArgumentException("Invalid rotation.");
        yaw = wrap(yaw);
        pitch = Math.max(-90, Math.min(90, wrap(pitch)));
      } else if (args.length > rest && args[rest].equals("facing")) {
        double[] face;
        if (args.length == rest + 4 && !args[rest + 1].equals("entity"))
          face =
              CommandNumbers.position(
                  self.x, self.boundingBox.minY, self.z, self.yaw, self.pitch, args, rest + 1);
        else if ((args.length == rest + 3 || args.length == rest + 4)
            && args[rest + 1].equals("entity")) {
          Entity dest = EntityTargets.one(source, args[rest + 2]);
          String anchor = args.length == rest + 4 ? args[rest + 3] : "feet";
          if (!Set.of("feet", "eyes").contains(anchor))
            throw new IllegalArgumentException("Choose feet or eyes.");
          face =
              new double[] {
                dest.x,
                anchor.equals("eyes")
                    ? dest.boundingBox.minY + dest.getEyeHeight()
                    : dest.boundingBox.minY,
                dest.z
              };
        } else
          throw new IllegalArgumentException(
              "Use facing <x y z> or facing entity <target> [feet|eyes].");
        double dx = face[0] - pos[0], dy = face[1] - pos[1], dz = face[2] - pos[2];
        yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
      } else if (args.length != rest)
        throw new IllegalArgumentException("Use /tp [targets] <x y z> [yaw pitch].");
      for (Entity target : targets)
        move(
            target,
            pos[0],
            pos[1],
            pos[2],
            yaw == null ? target.yaw : yaw,
            pitch == null ? target.pitch : pitch);
    }
    source.sendFeedback(
        "§aTeleported "
            + (targets.size() == 1
                ? EntityTargets.name(targets.get(0))
                : targets.size() + " entities")
            + ".");
  }

  private static float wrap(float value) {
    value %= 360;
    return value >= 180 ? value - 360 : value < -180 ? value + 360 : value;
  }

  private static void move(Entity target, double x, double y, double z, float yaw, float pitch) {
    if (target.vehicle != null) target.setVehicle(null);
    if (target instanceof PlayerEntity p
        && FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER)
      ServerUtil.serverTeleport(p, x, y, z);
    else target.setPositionAndAngles(x, y + target.standingEyeHeight, z, yaw, pitch);
    target.velocityX = target.velocityY = target.velocityZ = 0;
    target.fallDistance = 0;
  }

  public String name() {
    return "tp";
  }

  public void manual(SharedCommandSource s) {
    s.sendFeedback("/tp [targets] <x y z> [yaw pitch] | /teleport has the same syntax");
    s.sendFeedback("/tp [targets] <destination> | /tp [targets] <x y z> facing <x y z>");
    s.sendFeedback("/tp [targets] <x y z> facing entity <target> [feet|eyes]");
    s.sendFeedback(
        "Targets: player name, @s, @p, @a, @r or @e. Coordinates: numbers, ~ offsets or ^ local"
            + " offsets.");
  }

  public String[] suggestion(SharedCommandSource s, int n, String current, String total) {
    return CommandSuggestions.targetsAndCoordinates(s, current);
  }
}
