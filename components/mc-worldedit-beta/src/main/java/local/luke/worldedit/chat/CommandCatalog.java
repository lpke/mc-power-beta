package local.luke.worldedit.chat;

import java.util.*;
import local.luke.worldedit.core.BlockParser;
import local.luke.worldedit.core.EntityQuery;

public final class CommandCatalog {
  public static final List<String> COMMANDS =
      List.of(
          "up",
          "ceil",
          "ascend",
          "asc",
          "descend",
          "desc",
          "unstuck",
          "!",
          "thru",
          "jumpto",
          "j",
          "wand",
          "toggleeditwand",
          "pos1",
          "pos2",
          "hpos1",
          "hpos2",
          "sel",
          "desel",
          "deselect",
          "chunk",
          "size",
          "drawsel",
          "expand",
          "contract",
          "shift",
          "outset",
          "inset",
          "set",
          "replace",
          "re",
          "walls",
          "faces",
          "outline",
          "center",
          "overlay",
          "hollow",
          "fill",
          "fillr",
          "gmask",
          "count",
          "distr",
          "copy",
          "cut",
          "paste",
          "clearclipboard",
          "rotate",
          "flip",
          "stack",
          "move",
          "undo",
          "redo",
          "clearhistory",
          "cancel",
          "status",
          "toggleplace",
          "sphere",
          "hsphere",
          "cyl",
          "hcyl",
          "drain",
          "replacenear",
          "removenear",
          "removeabove",
          "removebelow",
          "help",
          "we",
          "worldedit",
          "remove",
          "rem",
          "rement",
          "butcher",
          "countentities");
  public static final Set<String> SINGLE =
      Set.of(
          "up",
          "ceil",
          "ascend",
          "asc",
          "descend",
          "desc",
          "unstuck",
          "!",
          "thru",
          "jumpto",
          "j",
          "we",
          "worldedit",
          "undo",
          "redo",
          "clearhistory",
          "clearclipboard",
          "toggleeditwand",
          "remove",
          "rem",
          "rement",
          "butcher");
  private static final List<String> DIRECTIONS =
      List.of("north", "south", "east", "west", "up", "down", "me");
  private static final List<String> BLOCKS = new BlockParser(i -> i <= 96).names();

  private CommandCatalog() {}

  public static boolean owns(String text) {
    String first = text.trim().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
    return first.startsWith("//") || first.startsWith("/") && SINGLE.contains(first.substring(1));
  }

  /** Complete the last token, retaining masks, weights and previous comma-separated entries. */
  public static List<String> complete(String input) {
    if (!input.startsWith("/")) return List.of();
    int space = input.lastIndexOf(' ');
    if (space < 0) {
      String prefix = input.startsWith("//") ? "//" : "/";
      Collection<String> names = prefix.equals("//") ? COMMANDS : SINGLE;
      return names.stream()
          .map(n -> prefix + n)
          .filter(n -> n.startsWith(input.toLowerCase(Locale.ROOT)))
          .sorted()
          .toList();
    }
    String[] parts = input.substring(0, space).trim().split("\\s+");
    String command = parts[0].replaceFirst("^/+", "").toLowerCase(Locale.ROOT);
    String token = input.substring(space + 1);
    int arg = (int) Arrays.stream(parts).skip(1).filter(p -> !p.startsWith("-")).count() + 1;
    Collection<String> choices = List.of();
    if (Set.of("up", "ceil").contains(command) && token.startsWith("-"))
      choices = List.of("-f", "-g");
    else if (Set.of("remove", "rem", "rement", "countentities").contains(command) && arg == 1)
      choices = EntityQuery.TYPES;
    else if (command.equals("butcher") && token.startsWith("-"))
      choices = List.of("-a", "-p", "-w", "-f", "-apw");
    else if (Set.of("flip", "expand", "contract", "shift", "move", "stack").contains(command))
      choices = command.equals("expand") && arg == 1 ? List.of("vert") : DIRECTIONS;
    else if (command.equals("sel")) choices = List.of("cuboid");
    else if (Set.of(
                    "set",
                    "walls",
                    "faces",
                    "outline",
                    "center",
                    "overlay",
                    "fill",
                    "fillr",
                    "sphere",
                    "hsphere",
                    "cyl",
                    "hcyl",
                    "removenear",
                    "count",
                    "gmask")
                .contains(command)
            && arg == 1
        || Set.of("replace", "re").contains(command) && arg <= 2
        || command.equals("replacenear") && arg >= 2 && arg <= 3
        || command.equals("hollow") && arg == 2) choices = BLOCKS;
    int start = Math.max(token.lastIndexOf(',') + 1, token.lastIndexOf('%') + 1);
    if (token.startsWith("!", start)) start++;
    String keep = input.substring(0, space + 1) + token.substring(0, start);
    String prefix = token.substring(start).toLowerCase(Locale.ROOT);
    return choices.stream().filter(n -> n.startsWith(prefix)).sorted().map(n -> keep + n).toList();
  }
}
