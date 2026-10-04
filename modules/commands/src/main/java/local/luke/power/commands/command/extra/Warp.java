package local.luke.power.commands.command.extra;

import java.util.*;
import local.luke.power.chat.HelpOutput;
import local.luke.power.commands.api.*;
import local.luke.power.commands.command.vanilla.Teleport;
import local.luke.power.commands.util.*;

public class Warp implements Command {
  private record Saved(String name, double x, double y, double z) {}

  private List<Saved> read(PlayerWarps player) {
    String value = player.spc$getWarpString();
    if (value == null || value.isBlank()) return new ArrayList<>();
    String[] parts = value.trim().split("\\s+");
    if (parts.length % 4 != 0)
      throw new IllegalArgumentException(
          "Saved warp data is incomplete. It has been left unchanged.");
    List<Saved> result = new ArrayList<>();
    for (int i = 0; i < parts.length; i += 4)
      result.add(
          new Saved(
              parts[i],
              CommandNumbers.number(parts[i + 1]),
              CommandNumbers.number(parts[i + 2]),
              CommandNumbers.number(parts[i + 3])));
    return result;
  }

  public void command(SharedCommandSource source, String[] args) {
    var player = EntityTargets.player(source);
    var data = (PlayerWarps) player;
    if (args.length < 2) {
      manual(source);
      return;
    }
    if (!Set.of("set", "tp", "list").contains(args[1]))
      throw new IllegalArgumentException("Use /warp set, /warp tp or /warp list.");
    if (args[1].equals("list")) {
      if (args.length > 3) {
        manual(source);
        return;
      }
      List<String> lines =
          read(data).stream().map(w -> w.name + " §8| §7" + w.x + " " + w.y + " " + w.z).toList();
      if (lines.isEmpty()) {
        source.sendFeedback("No saved warps. Use /warp set <name>.");
        return;
      }
      int page =
          args.length == 3 ? CommandNumbers.integer(args[2], 1, (lines.size() + 4) / 5, "Page") : 1;
      HelpOutput.print(
          source::sendFeedback,
          "Saved warps",
          lines,
          page,
          5,
          args.length == 2 && HelpOutput.scrolling());
      return;
    }
    if (args.length != 3) {
      manual(source);
      return;
    }
    if (args[2].contains("|") || args[2].length() > 64)
      throw new IllegalArgumentException(
          "Warp names must be at most 64 characters and cannot contain |.");
    List<Saved> saved = read(data);
    if (args[1].equals("set")) {
      if (saved.stream().anyMatch(w -> w.name.equals(args[2])))
        throw new IllegalArgumentException(
            "That warp already exists. Use another name to keep it unchanged.");
      // Keep the original serialization intact; only append a fully validated entry.
      String existing = data.spc$getWarpString();
      String entry =
          args[2]
              + " "
              + Double.toString(player.x)
              + " "
              + Double.toString(player.y)
              + " "
              + Double.toString(player.z)
              + " ";
      data.spc$setWarpString(
          (existing == null || existing.isBlank() ? "" : existing.trim() + " ") + entry);
      source.sendFeedback("§aSaved warp " + args[2] + ".");
      return;
    }
    Saved target =
        saved.stream()
            .filter(w -> w.name.equals(args[2]))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Warp not found: " + args[2]));
    if (Math.abs(target.x) >= 30000000
        || Math.abs(target.z) >= 30000000
        || Math.abs(target.y) > 4096)
      throw new IllegalArgumentException(
          "Warp is outside safe Beta bounds. Saved data is unchanged.");
    Teleport.teleport(player, target.x, target.y + .1, target.z);
    source.sendFeedback("§aTeleported to " + target.name + ".");
  }

  public String name() {
    return "warp";
  }

  public void manual(SharedCommandSource s) {
    s.sendFeedback("/warp set <name> | /warp tp <name> | /warp list [page]");
    s.sendFeedback(
        "Save your current position or return to it. Names are unique; existing warps are never"
            + " overwritten.");
  }

  public String[] suggestion(SharedCommandSource s, int n, String input, String total) {
    if (n == 1) return CommandSuggestions.suffix(input, List.of("set", "tp", "list"));
    if (n == 2 && total.contains(" tp "))
      return CommandSuggestions.suffix(
          input, read((PlayerWarps) EntityTargets.player(s)).stream().map(Saved::name).toList());
    return new String[0];
  }
}
