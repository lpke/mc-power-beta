package local.luke.power.commands.command.vanilla;

import java.util.*;
import local.luke.power.chat.HelpOutput;
import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.*;

public class Help implements Command {
  private List<String> lines(SharedCommandSource source) {
    boolean admin =
        source.isClient() || source.getPlayer() == null || ServerUtil.isOp(source.getName());
    Set<String> names = new HashSet<>();
    for (Command c : RetroChatUtil.commands) if (c.name() != null) names.add(c.name());
    List<String> result = new ArrayList<>();
    for (Command c : RetroChatUtil.commands) {
      String name = c.name();
      if (name == null
          || name.startsWith("/")
          || source.isClient() && c.disableInSingleplayer()
          || !admin && c.needsPermissions()) continue;
      if (Set.of("gm", "teleport").contains(name)) continue;
      if (names.contains("/" + name) && CommandHelp.description(name).isEmpty()) continue;
      String description = CommandHelp.description(name);
      result.add("/" + name + (description.isEmpty() ? "" : " §8- §7" + description));
    }
    result =
        result.stream()
            .distinct()
            .sorted()
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    if (names.contains("/help")) result.add("//help §8- §7World editing, selections and undo");
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
                      wanted.equals(c.name()) && (!source.isClient() || !c.disableInSingleplayer()))
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
                .filter(c -> !s.isClient() || !c.disableInSingleplayer())
                .map(Command::name)
                .filter(Objects::nonNull)
                .toList())
        : new String[0];
  }
}
