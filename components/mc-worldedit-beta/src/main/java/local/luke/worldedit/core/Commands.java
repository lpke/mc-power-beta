package local.luke.worldedit.core;

import static local.luke.worldedit.core.BlockParser.integer;
import static local.luke.worldedit.core.BlockParser.number;

import java.util.*;

public final class Commands {
  public interface Player {
    Pos feet();

    Pos target();

    Pos facing();

    void wand();

    void toggleWand();

    void drawSelection();

    default void navigate(String command, int amount, boolean flight, boolean glass) {
      throw new IllegalArgumentException("Navigation is unavailable.");
    }

    default int entities(EntityQuery query, boolean remove) {
      throw new IllegalArgumentException("Entity editing is unavailable.");
    }
  }

  private final Editor e;
  private final Player player;

  public Commands(Editor editor, Player player) {
    e = editor;
    this.player = player;
  }

  public void run(String input) {
    String[] split = input.trim().split("\\s+");
    String command = split[0].replaceFirst("^/+", "").toLowerCase(Locale.ROOT);
    List<String> a = new ArrayList<>(Arrays.asList(split).subList(1, split.length));
    if (e.engine.busy() && !Set.of("cancel", "help", "status").contains(command))
      throw new IllegalArgumentException("An edit is running. Use //cancel or wait.");
    Set<Character> flags = new HashSet<>();
    for (Iterator<String> it = a.iterator(); it.hasNext(); ) {
      String s = it.next();
      if (s.matches("-[a-zA-Z]+")) {
        for (char c : s.substring(1).toCharArray()) flags.add(c);
        it.remove();
      }
    }
    validFlags(
        flags,
        switch (command) {
          case "paste" -> "aos";
          case "stack", "move" -> "as";
          case "sphere", "hsphere" -> "r";
          case "outset", "inset" -> "hv";
          case "sel" -> "d";
          case "butcher" -> "apwf";
          case "up", "ceil" -> "fg";
          default -> "";
        });
    Pos origin =
        Set.of(
                    "fill",
                    "fillr",
                    "sphere",
                    "hsphere",
                    "cyl",
                    "hcyl",
                    "drain",
                    "replacenear",
                    "removenear",
                    "removeabove",
                    "removebelow")
                .contains(command)
            ? e.placement(player.feet())
            : player.feet();
    switch (command) {
      case "help", "we", "worldedit" -> {
        arity(a, 0, 1);
        help(a.isEmpty() ? 1 : integer(a.get(0), 1, 6, "page"));
      }
      case "remove", "rem", "rement", "countentities" -> {
        arity(a, 1, 2);
        EntityQuery query = EntityQuery.parse(a.get(0), a.size() == 2 ? a.get(1) : "32");
        boolean remove = !command.equals("countentities");
        int count = player.entities(query, remove);
        e.message.accept(
            (remove ? "Removed " : "Found ")
                + count
                + " entities within "
                + query.radius()
                + " blocks."
                + (remove ? " Entity removal cannot be undone." : ""));
      }
      case "butcher" -> {
        arity(a, 0, 1);
        EntityQuery query = EntityQuery.butcher(flags, a.isEmpty() ? "32" : a.get(0));
        int count = player.entities(query, true);
        e.message.accept("Removed " + count + " mobs. Entity removal cannot be undone.");
      }
      case "up",
          "ceil",
          "ascend",
          "asc",
          "descend",
          "desc",
          "unstuck",
          "!",
          "thru",
          "jumpto",
          "j" -> {
        boolean zero = Set.of("unstuck", "!", "thru", "jumpto", "j").contains(command);
        arity(a, command.equals("up") ? 1 : 0, zero ? 0 : 1);
        int amount =
            a.isEmpty()
                ? (command.equals("ceil") ? 0 : 1)
                : integer(a.get(0), command.equals("ceil") ? 0 : 1, 127, "distance or levels");
        player.navigate(command, amount, flags.contains('f'), flags.contains('g'));
      }
      case "status" ->
          e.message.accept(
              e.engine.busy()
                  ? "An edit is running."
                  : "Ready. Undo steps: " + e.engine.undoSize());
      case "cancel" -> {
        arity(a, 0, 0);
        e.engine.cancel();
      }
      case "wand" -> {
        arity(a, 0, 0);
        player.wand();
      }
      case "toggleeditwand" -> {
        arity(a, 0, 0);
        player.toggleWand();
      }
      case "drawsel" -> {
        arity(a, 0, 0);
        player.drawSelection();
      }
      case "pos1", "pos2" -> {
        e.position(command.equals("pos1") ? 1 : 2, coordinates(a, player.feet()));
      }
      case "hpos1", "hpos2" -> {
        arity(a, 0, 0);
        Pos p = player.target();
        if (p == null) throw new IllegalArgumentException("Aim at a block within reach.");
        e.position(command.equals("hpos1") ? 1 : 2, p);
      }
      case "sel", "desel", "deselect" -> {
        arity(a, 0, 1);
        if (!a.isEmpty() && !a.get(0).equals("cuboid"))
          throw new IllegalArgumentException("Only cuboid selections are supported.");
        e.pos1 = e.pos2 = null;
        e.message.accept("Selection cleared.");
      }
      case "chunk" -> {
        arity(a, 0, 0);
        Pos p = player.feet();
        e.select(
            new Region(
                new Pos((p.x() >> 4) << 4, 0, (p.z() >> 4) << 4),
                new Pos(((p.x() >> 4) << 4) + 15, 127, ((p.z() >> 4) << 4) + 15)));
        size();
      }
      case "size" -> {
        arity(a, 0, 0);
        size();
      }
      case "expand", "contract", "shift", "outset", "inset" -> selection(command, a, flags);
      case "set", "walls", "faces", "outline", "center" -> {
        arity(a, 1, 1);
        e.regionEdit(command.equals("outline") ? "faces" : command, a.get(0), null, 0);
      }
      case "replace", "re" -> {
        arity(a, 1, 2);
        e.regionEdit("set", a.get(a.size() - 1), a.size() == 1 ? "#existing" : a.get(0), 0);
      }
      case "hollow" -> {
        arity(a, 0, 2);
        e.regionEdit(
            "hollow",
            a.size() == 2 ? a.get(1) : "air",
            null,
            a.isEmpty() ? 1 : integer(a.get(0), 1, 128, "thickness"));
      }
      case "overlay" -> {
        arity(a, 1, 1);
        overlay(a.get(0));
      }
      case "copy", "cut" -> {
        arity(a, 0, 0);
        e.copy(player.feet(), command.equals("cut"));
      }
      case "paste" -> {
        arity(a, 0, 0);
        validFlags(flags, "aos");
        e.paste(player.feet(), flags.contains('a'), flags.contains('o'), flags.contains('s'));
      }
      case "rotate" -> {
        arity(a, 1, 1);
        clipboard();
        int degrees = integer(a.get(0), -36000, 36000, "degrees");
        if (degrees % 90 != 0)
          throw new IllegalArgumentException(
              "Beta clipboard rotation uses multiples of 90 degrees.");
        e.clipboard = e.clipboard.rotate(degrees / 90);
        e.message.accept("Clipboard rotated.");
      }
      case "flip" -> {
        arity(a, 0, 1);
        clipboard();
        Pos d = direction(a.isEmpty() ? "me" : a.get(0));
        e.clipboard = e.clipboard.flip(d.x() != 0 ? 'x' : d.y() != 0 ? 'y' : 'z');
        e.message.accept("Clipboard flipped.");
      }
      case "clearclipboard" -> {
        arity(a, 0, 0);
        e.clipboard = null;
        e.clipboardOrigin = null;
        e.message.accept("Clipboard cleared.");
      }
      case "stack", "move" -> {
        arity(a, 0, 2);
        validFlags(flags, "as");
        boolean move = command.equals("move");
        int count = a.isEmpty() ? 1 : integer(a.get(0), 1, 1024, "count");
        Pos d = direction(a.size() < 2 ? "me" : a.get(1));
        Region r = e.region();
        Pos delta =
            new Pos(
                d.x() * (move ? count : r.sizeX()),
                d.y() * (move ? count : r.sizeY()),
                d.z() * (move ? count : r.sizeZ()));
        e.duplicate(move ? 1 : count, delta, move, flags.contains('a'), flags.contains('s'));
      }
      case "undo", "redo" -> {
        arity(a, 0, 1);
        e.engine.history(
            command.equals("redo"), a.isEmpty() ? 1 : integer(a.get(0), 1, 100, "steps"));
      }
      case "clearhistory" -> {
        arity(a, 0, 0);
        e.engine.clearHistory();
        e.message.accept("History cleared.");
      }
      case "gmask" -> {
        arity(a, 0, 1);
        e.mask = a.isEmpty() ? b -> true : e.blocks.mask(a.get(0));
        e.message.accept(a.isEmpty() ? "Global mask cleared." : "Global mask set.");
      }
      case "count", "distr" -> {
        arity(a, command.equals("count") ? 1 : 0, 1);
        e.query(command, a.isEmpty() ? null : a.get(0));
      }
      case "fill", "fillr" -> {
        arity(a, 2, 3);
        Shapes.fill(
            e,
            origin,
            a.get(0),
            number(a.get(1), 1, 128, "radius"),
            a.size() == 3 ? integer(a.get(2), 1, 128, "depth") : command.equals("fill") ? 1 : 128,
            command.equals("fillr"));
      }
      case "sphere", "hsphere", "cyl", "hcyl" -> {
        arity(a, 2, 3);
        validFlags(flags, "r");
        double[] r = radii(a.get(1));
        boolean cylinder = command.endsWith("cyl");
        if (cylinder && r.length == 3)
          throw new IllegalArgumentException("Cylinder uses one or two radii.");
        if (!cylinder && a.size() > 2)
          throw new IllegalArgumentException("Use //sphere [-r] pattern radius[,radius,radius].");
        Shapes.generate(
            e,
            origin,
            command,
            a.get(0),
            r[0],
            cylinder ? 1 : r.length == 3 ? r[1] : r[0],
            r.length > 1 ? r[r.length - 1] : r[0],
            a.size() == 3 ? integer(a.get(2), 1, 128, "height") : 1,
            flags.contains('r'));
      }
      case "drain" -> {
        arity(a, 1, 1);
        int r = integer(a.get(0), 0, 128, "radius");
        Shapes.nearby(e, origin, r, "8,9,10,11", "air", r, r);
      }
      case "replacenear" -> {
        arity(a, 3, 3);
        int r = integer(a.get(0), 0, 128, "radius");
        Shapes.nearby(e, origin, r, a.get(1), a.get(2), r, r);
      }
      case "removenear" -> {
        arity(a, 2, 2);
        int r = integer(a.get(1), 0, 128, "radius");
        Shapes.nearby(e, origin, r, a.get(0), "air", r, r);
      }
      case "removeabove", "removebelow" -> {
        arity(a, 0, 2);
        int r = a.isEmpty() ? 1 : integer(a.get(0), 0, 128, "size"),
            h = a.size() < 2 ? 128 : integer(a.get(1), 1, 128, "height");
        Shapes.nearby(
            e,
            origin,
            r,
            "*",
            "air",
            command.equals("removeabove") ? h : 0,
            command.equals("removebelow") ? h : 0);
      }
      case "toggleplace" -> {
        arity(a, 0, 0);
        e.usePos1 = !e.usePos1;
        e.message.accept(e.usePos1 ? "Placement uses position 1." : "Placement uses your feet.");
      }
      default -> throw new IllegalArgumentException("Unknown WorldEdit Beta command. Use //help.");
    }
    if (!Set.of(
                "paste", "stack", "move", "sphere", "hsphere", "cyl", "hcyl", "outset", "inset",
                "sel", "butcher")
            .contains(command)
        && !flags.isEmpty()) throw new IllegalArgumentException("Unsupported command flag.");
  }

  private void overlay(String pattern) {
    Region r = e.region();
    e.bounded(r);
    var paint = e.blocks.pattern(pattern);
    Map<Pos, BlockValue> data = new LinkedHashMap<>();
    for (int x = r.min().x(); x <= r.max().x(); x++)
      for (int z = r.min().z(); z <= r.max().z(); z++)
        for (int y = r.max().y(); y >= r.min().y(); y--) {
          Pos p = new Pos(x, y, z);
          if (e.engine.read(p).id != 0) {
            Pos above = p.add(0, 1, 0);
            if (!above.valid()) throw new IllegalArgumentException("Overlay exceeds world height.");
            data.put(above, paint.apply(above));
            break;
          }
        }
    e.edit(data.keySet(), data::get);
  }

  private void selection(String command, List<String> a, Set<Character> flags) {
    Region r = e.region();
    if (command.equals("expand") && a.size() == 1 && a.get(0).equals("vert")) {
      e.select(
          new Region(new Pos(r.min().x(), 0, r.min().z()), new Pos(r.max().x(), 127, r.max().z())));
      size();
      return;
    }
    if (command.equals("outset") || command.equals("inset")) {
      arity(a, 1, 1);
      validFlags(flags, "hv");
      if (flags.size() > 1) throw new IllegalArgumentException("Choose -h or -v.");
      int n = integer(a.get(0), 0, 128, "amount") * (command.equals("inset") ? -1 : 1);
      int h = flags.contains('v') ? 0 : n, v = flags.contains('h') ? 0 : n;
      e.select(new Region(r.min().add(-h, -v, -h), r.max().add(h, v, h)));
      size();
      return;
    }
    arity(a, 1, 3);
    int n = integer(a.get(0), 0, 32000000, "amount"), back = 0;
    String dir = "me";
    if (a.size() >= 2) {
      if (a.get(1).matches("[0-9]+")) back = integer(a.get(1), 0, 32000000, "reverse amount");
      else dir = a.get(1);
    }
    if (a.size() == 3) dir = a.get(2);
    Pos d = direction(dir);
    if (command.equals("shift")) {
      if (back != 0) throw new IllegalArgumentException("Use //shift amount direction.");
      e.select(r.shift(new Pos(d.x() * n, d.y() * n, d.z() * n)));
    } else {
      if (command.equals("contract")) {
        n = -n;
        back = -back;
      }
      int dx = d.x(), dy = d.y(), dz = d.z();
      e.select(
          new Region(
              r.min()
                  .add(
                      dx < 0 ? -n : dx > 0 ? -back : 0,
                      dy < 0 ? -n : dy > 0 ? -back : 0,
                      dz < 0 ? -n : dz > 0 ? -back : 0),
              r.max()
                  .add(
                      dx > 0 ? n : dx < 0 ? back : 0,
                      dy > 0 ? n : dy < 0 ? back : 0,
                      dz > 0 ? n : dz < 0 ? back : 0)));
    }
    size();
  }

  private void clipboard() {
    if (e.clipboard == null) throw new IllegalArgumentException("Clipboard is empty.");
  }

  private void size() {
    Region r = e.region();
    e.message.accept(
        r.sizeX() + " x " + r.sizeY() + " x " + r.sizeZ() + " = " + r.volume() + " blocks.");
  }

  private static void arity(List<String> a, int min, int max) {
    if (a.size() < min || a.size() > max)
      throw new IllegalArgumentException("Wrong arguments. Use //help for command syntax.");
  }

  private static void validFlags(Set<Character> flags, String valid) {
    for (char c : flags)
      if (valid.indexOf(c) < 0) throw new IllegalArgumentException("Unsupported flag: -" + c);
  }

  public Pos direction(String s) {
    return switch (s.toLowerCase(Locale.ROOT)) {
      case "up", "u" -> new Pos(0, 1, 0);
      case "down", "d" -> new Pos(0, -1, 0);
      case "north", "n" -> new Pos(0, 0, -1);
      case "south", "s" -> new Pos(0, 0, 1);
      case "east", "e" -> new Pos(1, 0, 0);
      case "west", "w" -> new Pos(-1, 0, 0);
      case "me", "forward", "f" -> player.facing();
      case "back" -> {
        Pos d = player.facing();
        yield new Pos(-d.x(), -d.y(), -d.z());
      }
      default ->
          throw new IllegalArgumentException("Use north, south, east, west, up, down or me.");
    };
  }

  public static Pos coordinates(List<String> a, Pos base) {
    if (a.isEmpty()) return base;
    String[] p = a.size() == 1 ? a.get(0).split(",", -1) : a.toArray(String[]::new);
    if (p.length != 3)
      throw new IllegalArgumentException("Use x,y,z or x y z; ~ coordinates are supported.");
    return new Pos(coord(p[0], base.x()), coord(p[1], base.y()), coord(p[2], base.z()));
  }

  private static int coord(String s, int base) {
    return s.startsWith("~")
        ? Math.addExact(
            base, s.length() == 1 ? 0 : integer(s.substring(1), -32000000, 32000000, "coordinate"))
        : integer(s, -32000000, 32000000, "coordinate");
  }

  private static double[] radii(String s) {
    String[] parts = s.split(",", -1);
    if (parts.length > 3) throw new IllegalArgumentException("Use at most three radii.");
    double[] r = new double[parts.length];
    for (int i = 0; i < r.length; i++) r[i] = number(parts[i], .5, 128, "radius");
    return r;
  }

  private void help(int page) {
    String[][] help = {
      {
        "//wand | //toggleeditwand | //pos1 [x,y,z] | //pos2 [x,y,z]",
        "//hpos1 | //hpos2 | //sel | //chunk | //size | //drawsel",
        "//expand amount [reverse] [direction] | //expand vert",
        "//contract amount [direction] | //shift amount [direction]",
        "//outset [-h|-v] amount | //inset [-h|-v] amount"
      },
      {
        "//set pattern | //replace [mask] pattern",
        "//walls pattern | //faces pattern | //center pattern",
        "//overlay pattern | //hollow [thickness] [pattern]",
        "//fill pattern radius [depth] | //fillr pattern radius [depth]",
        "//gmask [mask] | //count mask | //distr"
      },
      {
        "//copy | //cut | //paste [-aos] | //clearclipboard",
        "//rotate degrees | //flip [direction]",
        "//stack [-as] [count] [direction] | //move [-as] [distance] [direction]",
        "//undo [steps] | //redo [steps] | //clearhistory",
        "//cancel | //status | //toggleplace"
      },
      {
        "//sphere [-r] pattern radius | //hsphere [-r] pattern radius",
        "//cyl pattern radius [height] | //hcyl pattern radius [height]",
        "//drain radius | //replacenear radius mask pattern",
        "//removenear mask radius | //removeabove [size] [height]",
        "//removebelow [size] [height]",
        "Patterns: stone, 44:2, 75%stone,25%cobble. Masks: !air, 44:2"
      },
      {
        "/remove type[,type] [radius=32] | //countentities type [radius]",
        "/butcher [-apwf] [radius=32]: hostile mobs; -a animals, -p pets",
        "-w water mobs, -f all friendly mobs. Radius: 1 to 256.",
        "Types: items, mobs, hostile, animals, pets, water, projectiles,",
        "vehicles, boats, minecarts, tnt, fallingblocks, paintings, all.",
        "Loaded entities only; players and their mounts are protected.",
        "Pets need pets/all or -p/-f. Removal has no drops or undo."
      },
      {
        "//up [-fg] distance | /ceil [-fg] [clearance]",
        "/ascend [levels] | /descend [levels] | /unstuck",
        "/jumpto or /j: stand above the target | /thru: pass through a wall",
        "-f uses creative flight; -g adds an undoable glass platform.",
        "Single- and double-slash forms work. Loaded chunks only."
      }
    };
    e.message.accept("WorldEdit Beta help " + page + "/" + help.length);
    for (String line : help[page - 1]) e.message.accept(line);
  }
}
