package local.luke.power.worldedit.core;

import java.util.*;

/** Named Beta states using WorldEdit's block[property=value] spelling. */
public final class BlockStates {
  private record Property(int mask, Map<String, Integer> values) {}
  private static Property values(int mask, String... names) {
    Map<String, Integer> values = new LinkedHashMap<>();
    for (int i = 0; i < names.length; i++) if (!names[i].isEmpty()) values.put(names[i], i);
    return new Property(mask, values);
  }
  private static Property bool(int bit) { return new Property(bit, Map.of("false", 0, "true", bit)); }
  private static Property numbers(int mask, int min, int max, int offset, int shift) {
    Map<String, Integer> values = new LinkedHashMap<>();
    for (int i = min; i <= max; i++) values.put("" + i, (i - offset) << shift);
    return new Property(mask, values);
  }
  private static Map<String, Property> properties(int id) {
    Map<String, Property> p = new LinkedHashMap<>();
    switch (id) {
      case 27, 28, 66 -> {
        p.put("shape", id == 66
            ? values(15, "north_south", "east_west", "ascending_east", "ascending_west", "ascending_north", "ascending_south", "south_east", "south_west", "north_west", "north_east")
            : values(7, "north_south", "east_west", "ascending_east", "ascending_west", "ascending_north", "ascending_south"));
        if (id != 66) p.put("powered", bool(8));
      }
      case 50, 75, 76 -> {
        p.put("facing", values(7, "", "east", "west", "south", "north", "up"));
        if (id != 50) p.put("lit", bool(0));
      }
      case 93, 94 -> {
        p.put("facing", values(3, "north", "east", "south", "west"));
        p.put("delay", numbers(12, 1, 4, 1, 2)); p.put("powered", bool(0));
      }
      case 23, 54, 61, 62, 65, 68 -> {
        p.put("facing", values(7, "", "", "north", "south", "west", "east"));
        if (id == 61 || id == 62) p.put("lit", bool(0));
      }
      case 29, 33 -> {
        p.put("facing", values(7, "down", "up", "north", "south", "west", "east"));
        p.put("extended", bool(8));
      }
      case 53, 67 -> {
        p.put("facing", values(3, "east", "west", "south", "north"));
        p.put("half", values(0, "bottom"));
      }
      case 86, 91 -> p.put("facing", values(3, "south", "west", "north", "east"));
      case 63 -> p.put("rotation", numbers(15, 0, 15, 0, 0));
      case 55 -> p.put("power", numbers(15, 0, 15, 0, 0));
      case 59 -> p.put("age", numbers(7, 0, 7, 0, 0));
      case 60 -> p.put("moisture", numbers(7, 0, 7, 0, 0));
      case 77 -> { p.put("facing", values(7, "", "east", "west", "south", "north")); p.put("powered", bool(8)); }
      case 70, 72 -> p.put("powered", bool(1));
      case 96 -> { p.put("facing", values(3, "north", "south", "west", "east")); p.put("open", bool(4)); p.put("half", values(0, "bottom")); }
      default -> {}
    }
    return p;
  }
  public static Map<String, String> parse(String text) {
    Map<String, String> result = new LinkedHashMap<>();
    if (text.isEmpty()) throw new IllegalArgumentException("Specify a block state inside [].");
    for (String field : text.split(",", -1)) {
      String[] pair = field.split("=", -1);
      if (pair.length != 2 || pair[0].isEmpty() || pair[1].isEmpty() || result.putIfAbsent(pair[0], pair[1]) != null)
        throw new IllegalArgumentException("Use unique property=value block states.");
    }
    return result;
  }
  public static BlockValue apply(BlockValue block, Map<String, String> states) {
    int id = block.id, meta = block.meta;
    var available = properties(id);
    for (var state : states.entrySet()) {
      Property property = available.get(state.getKey());
      Integer bits = property == null ? null : property.values.get(state.getValue());
      if (bits == null) throw new IllegalArgumentException("Unsupported Beta state: " + state.getKey() + "=" + state.getValue());
      meta = (meta & ~property.mask) | bits;
      if (state.getKey().equals("lit")) {
        boolean lit = state.getValue().equals("true");
        id = switch (id) { case 75, 76 -> lit ? 76 : 75; case 61, 62 -> lit ? 62 : 61; default -> id; };
      } else if (state.getKey().equals("powered") && (id == 93 || id == 94)) id = state.getValue().equals("true") ? 94 : 93;
    }
    return new BlockValue(id, meta, block.nbt());
  }
  public static int mask(int id, Set<String> states) {
    int mask = 0; var properties = properties(id);
    for (String name : states) mask |= properties.get(name).mask;
    return mask;
  }
  public static List<String> complete(int id, String prefix) {
    int comma = prefix.lastIndexOf(',');
    String keep = prefix.substring(0, comma + 1), tail = prefix.substring(comma + 1);
    Set<String> used = new HashSet<>();
    for (String s : keep.split(",")) if (s.contains("=")) used.add(s.substring(0, s.indexOf('=')));
    List<String> choices = new ArrayList<>();
    properties(id).forEach((name, property) -> {
      if (!used.contains(name)) for (String value : property.values.keySet()) {
        String entry = name + "=" + value;
        if (entry.startsWith(tail)) choices.add(keep + entry + "]");
      }
    });
    return choices.stream().sorted().toList();
  }
  /** Commas inside brackets separate properties, not pattern or mask entries. */
  public static List<String> split(String text) {
    List<String> parts = new ArrayList<>(); int start = 0, depth = 0;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == '[' && ++depth > 1 || c == ']' && --depth < 0) throw new IllegalArgumentException("Invalid block state brackets.");
      if (c == ',' && depth == 0) { parts.add(text.substring(start, i)); start = i + 1; }
    }
    if (depth != 0) throw new IllegalArgumentException("Close block states with ].");
    parts.add(text.substring(start)); return parts;
  }
}
