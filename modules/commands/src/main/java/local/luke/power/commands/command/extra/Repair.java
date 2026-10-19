package local.luke.power.commands.command.extra;

import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.*;

public final class Repair implements Command {
  public String name() { return "repair"; }
  public void command(SharedCommandSource source, String[] args) {
    if (args.length != 1) { manual(source); return; }
    var player = EntityTargets.player(source);
    var stack = player.inventory.getSelectedItem();
    if (stack == null) { source.sendFeedback("§7Hold a tool or piece of armor to repair it."); return; }
    if (!stack.isDamageable()) { source.sendFeedback("§7That item cannot be repaired."); return; }
    if (stack.getDamage() == 0) { source.sendFeedback("§7That item is already fully repaired."); return; }
    stack.setDamage(0);
    player.inventory.markDirty();
    source.sendFeedback("§aRepaired the held item.");
  }
  public void manual(SharedCommandSource source) {
    source.sendFeedback("§b/repair §7Fully repair the held tool or armor.");
    source.sendFeedback("§7Requires allow_survival_assistance. Other item data stays unchanged.");
  }
}
