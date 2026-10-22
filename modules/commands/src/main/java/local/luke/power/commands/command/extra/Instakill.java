package local.luke.power.commands.command.extra;

import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.EntityTargets;
import local.luke.power.commands.util.SharedCommandSource;
import local.luke.power.input.InstantKill;

public final class Instakill implements Command {
  private final String name;

  public Instakill(String name) { this.name = name; }
  public String name() { return name; }

  public void command(SharedCommandSource source, String[] args) {
    if (args.length != 1) { manual(source); return; }
    var player = EntityTargets.player(source);
    if (player.world.isRemote) throw new IllegalArgumentException("Instakill is singleplayer only.");
    boolean enabled = InstantKill.toggle(player, player.world);
    source.sendFeedback("§7Instakill " + (enabled ? "enabled" : "disabled") + ".");
  }

  public void manual(SharedCommandSource source) {
    source.sendFeedback("§b/instakill §7Toggle one-hit attacks. Alias: /ik.");
    source.sendFeedback("§7Requires allow_survival_assistance. Clears when leaving the world.");
  }
}
