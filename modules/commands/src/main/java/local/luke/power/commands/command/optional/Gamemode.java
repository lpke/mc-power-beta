package local.luke.power.commands.command.optional;

import java.util.ArrayList;
import local.luke.power.commands.api.Command;
import local.luke.power.commands.optionaldep.power_creative_inventory.ChangeGamemode;
import local.luke.power.commands.util.SharedCommandSource;

public class Gamemode implements Command {
  @Override
  public void command(SharedCommandSource commandSource, String[] parameters) {
    if (commandSource.getPlayer() == null) {
      commandSource.sendFeedback("Can only be used by a player.");
      return;
    }

    if (parameters.length > 1) {
      String mode =
          parameters[1].equals("creative") || parameters[1].equals("c") || parameters[1].equals("1")
              ? "creative"
              : "survival";
      String denial = local.luke.power.permissions.CommandPermissions.modeDenial(mode);
      if (!denial.isEmpty()) {
        commandSource.sendFeedback("§c" + denial);
        return;
      }
      if (parameters[1].charAt(0) == 's' || parameters[1].charAt(0) == '0') {
        commandSource.sendFeedback("Set game mode to Survival Mode");
        ChangeGamemode.set(commandSource.getPlayer(), false);
      } else if (parameters[1].charAt(0) == 'c' || parameters[1].charAt(0) == '1') {
        commandSource.sendFeedback("Set game mode to Creative Mode");
        ChangeGamemode.set(commandSource.getPlayer(), true);
      }
      return;
    }

    manual(commandSource);
  }

  @Override
  public String name() {
    return "gamemode";
  }

  @Override
  public void manual(SharedCommandSource commandSource) {
    commandSource.sendFeedback("Requires: CreativeInventory");
    commandSource.sendFeedback("Usage: /gamemode {mode}");
    commandSource.sendFeedback("Info: changes player's gamemode");
    commandSource.sendFeedback("mode can be 0/s/survival for survival mode");
    commandSource.sendFeedback("mode can be 1/c/creative for creative mode");
  }

  @Override
  public String[] suggestion(
      SharedCommandSource source, int parameterNum, String currentInput, String totalInput) {
    if (parameterNum == 1) {
      String[] options = {"survival", "creative"};
      ArrayList<String> output = new ArrayList<>();
      for (String option : options) {
        if (option.startsWith(currentInput)) {
          output.add(option.substring(currentInput.length()));
        }
      }
      return output.toArray(new String[0]);
    }
    return new String[0];
  }
}
