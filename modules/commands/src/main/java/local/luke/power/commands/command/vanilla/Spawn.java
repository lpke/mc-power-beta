package local.luke.power.commands.command.vanilla;

import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.*;

public final class Spawn implements Command {
  public String name() { return "spawn"; }
  public void command(SharedCommandSource source, String[] args) {
    if (args.length != 1) { manual(source); return; }
    var player = EntityTargets.player(source);
    if (local.luke.power.input.InteractionState.carryingContainer)
      throw new IllegalArgumentException("Place your carried container before travelling to spawn.");
    if (player.dimensionId != 0) {
      if (!Teleport.switchDimensions(source, "minecraft:overworld"))
        throw new IllegalArgumentException("Return to the Overworld before using /spawn.");
      player = source.isClient()
          ? ((net.minecraft.client.Minecraft)net.fabricmc.loader.api.FabricLoader.getInstance().getGameInstance()).player
          : EntityTargets.player(source);
      if (player.dimensionId != 0) throw new IllegalArgumentException("Could not reach the Overworld.");
    }
    var pos = player.world.getSpawnPos();
    if (player.vehicle != null) player.setVehicle(null);
    Teleport.teleport(player, pos.x + .5, pos.y + player.standingEyeHeight, pos.z + .5);
    source.sendFeedback("§aTeleported to world spawn.");
  }
  public void manual(SharedCommandSource source) {
    source.sendFeedback("/spawn | /spawn help");
    source.sendFeedback("Teleport to the Overworld's world spawn. Requires allow_teleportation. Your personal respawn point stays unchanged.");
  }
  public String[] suggestion(SharedCommandSource s, int n, String current, String total) {
    return n == 1 ? CommandSuggestions.suffix(current, java.util.List.of("help")) : new String[0];
  }
}
