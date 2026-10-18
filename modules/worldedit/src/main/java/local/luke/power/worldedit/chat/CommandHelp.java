package local.luke.power.worldedit.chat;

import java.util.*;
import java.util.function.*;
import local.luke.power.chat.HelpOutput;

/** Short individual syntax entries also form the permission-filtered help index. */
public final class CommandHelp {
  private static final Map<String, String> SYNTAX = new LinkedHashMap<>();
  static {
    add("help", "[page]", "List available editing commands.");
    add("calc", "<expression>", "Calculate arithmetic, powers and functions.");
    add("wand", "", "Give a selection wand, or select one you own.");
    add("toggleeditwand", "", "Toggle the selection wand.");
    add("drawsel", "", "Toggle the selection outline.");
    for (String name : List.of("pos1", "pos2")) add(name, "[x,y,z]", "Set a selection corner at your reported coordinates, including Spectator and freecam.");
    for (String name : List.of("hpos1", "hpos2")) add(name, "", "Set a selection corner at the block under your current view, including Spectator and freecam.");
    add("sel", "[cuboid]", "Clear the selection."); add("chunk", "", "Select the current chunk, from bedrock to sky.");
    add("size", "", "Show selection dimensions and volume.");
    add("expand", "<amount> [reverse] [direction] | vert", "Expand the selection.");
    add("contract", "<amount> [direction]", "Contract the selection.");
    add("shift", "<amount> [direction]", "Move the selection without moving blocks.");
    for (String name : List.of("outset", "inset")) add(name, "[-hv] <amount>", "Resize every side; -h horizontal, -v vertical.");
    for (String name : List.of("set", "walls", "faces", "center", "overlay")) add(name, "<pattern>", "Place a block pattern in the selection.");
    add("replace", "[mask] <pattern>", "Replace matching blocks."); add("hollow", "[thickness] [pattern]", "Hollow the selection.");
    for (String name : List.of("fill", "fillr")) add(name, "<pattern> <radius> [depth]", "Fill connected empty space.");
    add("gmask", "[mask]", "Set or clear the edit mask."); add("count", "<mask>", "Count matching blocks."); add("distr", "", "Show the selected block distribution.");
    add("copy", "", "Copy selected blocks to the clipboard."); add("cut", "", "Copy selected blocks and remove them.");
    add("paste", "[-aos]", "Paste; -a skips air, -o uses original position, -s selects result.");
    add("clearclipboard", "", "Discard the clipboard."); add("rotate", "<degrees>", "Rotate the clipboard in 90-degree steps."); add("flip", "[direction]", "Flip the clipboard.");
    add("stack", "[-as] [count] [direction]", "Repeat the selection; -a skips air, -s selects result.");
    add("move", "[-as] [distance] [direction]", "Move selected blocks; -a skips air, -s selects result.");
    for (String name : List.of("undo", "redo")) add(name, "[steps]", "Restore blocks from edit history.");
    add("clearhistory", "", "Forget undo and redo history."); add("cancel", "", "Cancel the current edit and finish rollback."); add("status", "", "Show edit progress and undo count.");
    add("toggleplace", "", "Switch placement origin between player and selection.");
    for (String name : List.of("sphere", "hsphere")) add(name, "[-r] <pattern> <radius>", "Build a sphere; -r raises its centre.");
    for (String name : List.of("cyl", "hcyl")) add(name, "<pattern> <radius> [height]", "Build a cylinder.");
    add("drain", "<radius>", "Remove nearby water and lava."); add("replacenear", "<radius> <mask> <pattern>", "Replace nearby blocks.");
    add("removenear", "<mask> <radius>", "Remove matching nearby blocks.");
    for (String name : List.of("removeabove", "removebelow")) add(name, "[size] [height]", "Remove blocks above or below the placement origin.");
    add("remove", "<type[,type]> [radius=32]", "Remove loaded entities without drops or undo. Players and mounts are protected.");
    add("countentities", "<type[,type]> [radius=32]", "Count loaded matching entities. Radius: 1 to 256.");
    add("butcher", "[-apwf] [radius=32]", "Remove monsters; -a animals, -p pets, -w water mobs, -f all friendly mobs.");
    add("up", "[-fg] <distance>", "Move upward; -f uses Creative flight, -g adds a glass platform requiring world editing.");
    add("ceil", "[-fg] [clearance]", "Move below the ceiling; glass platforms require world editing.");
    add("ascend", "[levels]", "Move up to a walkable floor."); add("descend", "[levels]", "Move down to a walkable floor.");
    add("unstuck", "", "Move to nearby open space."); add("thru", "", "Pass through the targeted wall."); add("jumpto", "", "Stand above the targeted block.");
  }
  private CommandHelp() {}
  private static void add(String name, String arguments, String description) { SYNTAX.put(name, "//" + name + (arguments.isEmpty() ? "" : " " + arguments) + " : " + description); }
  public static String line(String name) { return SYNTAX.get(CommandAccess.canonical(name)); }
  public static void manual(String name, Consumer<String> output) {
    String line = line(name);
    if (line == null) throw new IllegalArgumentException("Unknown editing command: " + name);
    output.accept(HelpOutput.line(line)); output.accept("§7" + CommandAccess.requirement(name) + "§r");
    String canonical = CommandAccess.canonical(name);
    var aliases = CommandCatalog.COMMANDS.stream().filter(n -> !n.equals(canonical) && CommandAccess.canonical(n).equals(canonical)).toList();
    if (!aliases.isEmpty()) output.accept("§7Aliases: " + String.join(", ", aliases) + "§r");
  }
  public static void index(int page, boolean all, Predicate<String> available, Consumer<String> output) {
    List<String> lines = SYNTAX.entrySet().stream().filter(e -> available.test(e.getKey())).map(Map.Entry::getValue).toList();
    int pages = Math.max(1, (lines.size() + 7) / 8);
    if (page < 1 || page > pages) throw new IllegalArgumentException("Choose a help page from 1 to " + pages + ".");
    HelpOutput.print(output, "World editing", lines, page, 8, all);
  }
}
