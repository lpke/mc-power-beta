package local.luke.power.commands.command.vanilla;

import java.util.*;
import local.luke.power.chat.HelpOutput;
import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.*;

public class Help implements Command {
  public List<String> lines(SharedCommandSource source) {
    List<String> result = new ArrayList<>();
    for (Command c : RetroChatUtil.commands) {
      String name = c.name();
      if (name == null
          || !CommandAccess.available(c, source)) continue;
      if (!c.helpGroup().isEmpty() && !name.equals("/help")) continue;
      String description = CommandHelp.description(name);
      if (name.equals("/help")) description = "World editing, selections and undo";
      result.add("/" + name + (description.isEmpty() ? "" : " §8- §7" + description));
    }
    result =
        result.stream()
            .distinct()
            .sorted()
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    return result;
  }

  public void command(SharedCommandSource source, String[] args) {
    if (args.length > 2) {
      manual(source);
      return;
    }
    if (args.length == 2 && !args[1].matches("[+-]?[0-9]+")) {
      String wanted = args[1].replaceFirst("^/", "");
      Command target =
          RetroChatUtil.commands.stream()
              .filter(
                  c ->
                      wanted.equals(c.name()) && CommandAccess.available(c, source))
              .findFirst()
              .orElse(null);
      if (target == null) throw new IllegalArgumentException("Unknown command: " + args[1]);
      source.sendFeedback("§6Command help §8| §b/" + wanted);
      target.manual(source);
      return;
    }
    var lines = lines(source);
    int page =
        args.length == 2
            ? CommandNumbers.integer(args[1], 1, Math.max(1, (lines.size() + 5) / 6), "Help page")
            : 1;
    HelpOutput.print(
        source::sendFeedback,
        "Commands",
        lines,
        page,
        6,
        args.length == 1 && source.isClient() && HelpOutput.scrolling());
  }

  public String name() {
    return "help";
  }

  public boolean needsPermissions() {
    return false;
  }

  public void manual(SharedCommandSource s) {
    s.sendFeedback("/help [page|command]");
    s.sendFeedback(
        "With chat scrolling enabled, /help shows the complete list. A page number always shows one"
            + " page.");
  }

  public String[] suggestion(SharedCommandSource s, int n, String input, String total) {
    return n == 1
        ? CommandSuggestions.suffix(
            input,
            RetroChatUtil.commands.stream()
                .filter(c -> CommandAccess.available(c, s))
                .map(Command::name)
                .filter(Objects::nonNull)
                .toList())
        : new String[0];
  }
}
