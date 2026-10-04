package local.luke.power.commands.command.extra;

import static local.luke.power.commands.util.ParameterSuggestUtil.suggestItemIdentifier;

import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.SharedCommandSource;

public class Id implements Command {
  @Override
  public void command(SharedCommandSource commandSource, String[] parameters) {
    if (parameters.length == 2) {
      var item = local.luke.power.commands.util.ItemArgument.parse(parameters[1]);
      commandSource.sendFeedback(
          "§b" + item.item().getTranslatedName() + "§7: " + item.item().id + ":" + item.metadata());
      return;
    }

    manual(commandSource);
  }

  @Override
  public String name() {
    return "id";
  }

  @Override
  public void manual(SharedCommandSource commandSource) {
    commandSource.sendFeedback("Usage: /id {item name/id}");
    commandSource.sendFeedback("Info: get the ID number of a given item");
    commandSource.sendFeedback(
        "item name: use the translated item name without spaces, or with '_' instead");
  }

  @Override
  public String[] suggestion(
      SharedCommandSource source, int parameterNum, String currentInput, String totalInput) {
    if (parameterNum == 1) {
      return suggestItemIdentifier(currentInput);
    }
    return new String[0];
  }

  public boolean needsPermissions() {
    return false;
  }
}
