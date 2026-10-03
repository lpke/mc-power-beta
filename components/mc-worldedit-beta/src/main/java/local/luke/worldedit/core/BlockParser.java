package local.luke.worldedit.core;

import java.util.*;
import java.util.function.*;

public final class BlockParser {
  private final Map<String, Integer> names = new HashMap<>();
  private final Map<String, Integer> defaultMeta = new HashMap<>();
  private final IntPredicate registered;

  public BlockParser(IntPredicate registered) {
    this.registered = registered;
    aliases();
    String[] colors = {
      "white",
      "orange",
      "magenta",
      "light_blue",
      "yellow",
      "lime",
      "pink",
      "gray",
      "light_gray",
      "cyan",
      "purple",
      "blue",
      "brown",
      "green",
      "red",
      "black"
    };
    for (int i = 0; i < colors.length; i++) state(colors[i] + "_wool", 35, i);
    String[] slabs = {"stone_slab", "sandstone_slab", "oak_slab", "cobblestone_slab"};
    for (int i = 0; i < slabs.length; i++) state(slabs[i], 44, i);
    state("spruce_log", 17, 1);
    state("birch_log", 17, 2);
    state("spruce_leaves", 18, 1);
    state("birch_leaves", 18, 2);
    state("spruce_sapling", 6, 1);
    state("birch_sapling", 6, 2);
  }

  public java.util.List<String> names() {
    return names.keySet().stream()
        .filter(
            n -> {
              int id = names.get(n);
              return id != 34 && id != 36 && id != 95 && (id == 0 || registered.test(id));
            })
        .sorted()
        .toList();
  }

  public void alias(String name, int id) {
    names.put(name.toLowerCase(Locale.ROOT), id);
  }

  private void state(String name, int id, int meta) {
    alias(name, id);
    defaultMeta.put(name, meta);
  }

  private void aliases() {
    String[] blocks = {
      "air",
      "stone",
      "grass_block",
      "dirt",
      "cobblestone",
      "oak_planks",
      "oak_sapling",
      "bedrock",
      "flowing_water",
      "water",
      "flowing_lava",
      "lava",
      "sand",
      "gravel",
      "gold_ore",
      "iron_ore",
      "coal_ore",
      "oak_log",
      "oak_leaves",
      "sponge",
      "glass",
      "lapis_ore",
      "lapis_block",
      "dispenser",
      "sandstone",
      "note_block",
      "bed",
      "powered_rail",
      "detector_rail",
      "sticky_piston",
      "cobweb",
      "tall_grass",
      "dead_bush",
      "piston",
      "piston_head",
      "wool",
      "moving_piston",
      "dandelion",
      "rose",
      "brown_mushroom",
      "red_mushroom",
      "gold_block",
      "iron_block",
      "double_slab",
      "slab",
      "bricks",
      "tnt",
      "bookshelf",
      "mossy_cobblestone",
      "obsidian",
      "torch",
      "fire",
      "spawner",
      "oak_stairs",
      "chest",
      "redstone_wire",
      "diamond_ore",
      "diamond_block",
      "crafting_table",
      "wheat",
      "farmland",
      "furnace",
      "lit_furnace",
      "sign",
      "wooden_door",
      "ladder",
      "rail",
      "cobblestone_stairs",
      "wall_sign",
      "lever",
      "stone_pressure_plate",
      "iron_door",
      "wooden_pressure_plate",
      "redstone_ore",
      "lit_redstone_ore",
      "unlit_redstone_torch",
      "redstone_torch",
      "stone_button",
      "snow",
      "ice",
      "snow_block",
      "cactus",
      "clay",
      "sugar_cane",
      "jukebox",
      "oak_fence",
      "pumpkin",
      "netherrack",
      "soul_sand",
      "glowstone",
      "nether_portal",
      "jack_o_lantern",
      "cake",
      "repeater",
      "powered_repeater",
      "locked_chest",
      "trapdoor"
    };
    for (int i = 0; i < blocks.length; i++) alias(blocks[i], i);
    String[] pairs = {
      "grass=2",
      "cobble=4",
      "planks=5",
      "wood=5",
      "log=17",
      "leaves=18",
      "lapis=22",
      "workbench=58",
      "craftingtable=58",
      "fence=85",
      "lightstone=89",
      "step=44",
      "doublestep=43",
      "brick=45",
      "mossycobble=48",
      "glass=20",
      "woodstairs=53",
      "redstone=55",
      "rails=66",
      "wooddoor=64"
    };
    for (String pair : pairs) {
      String[] a = pair.split("=");
      alias(a[0], Integer.parseInt(a[1]));
    }
  }

  public BlockValue block(String text) {
    String v = text.toLowerCase(Locale.ROOT).replace("minecraft:", "");
    String[] parts = v.split(":", -1);
    if (parts.length > 2 || parts[0].isBlank())
      throw new IllegalArgumentException("Use a block name or ID[:metadata].");
    int id;
    try {
      id = Integer.parseInt(parts[0]);
    } catch (NumberFormatException e) {
      Integer known = names.get(parts[0]);
      if (known == null) throw new IllegalArgumentException("Unknown Beta block: " + parts[0]);
      id = known;
    }
    int meta =
        parts.length == 2
            ? integer(parts[1], 0, 15, "metadata")
            : defaultMeta.getOrDefault(parts[0], 0);
    if (id < 0 || id > 255 || id == 34 || id == 36 || id == 95 || id != 0 && !registered.test(id))
      throw new IllegalArgumentException("Unsupported Beta block: " + text);
    return new BlockValue(id, meta);
  }

  public Function<Pos, BlockValue> pattern(String text) {
    String[] tokens = text.split(",", -1);
    if (tokens.length > 128) throw new IllegalArgumentException("Pattern is too long.");
    List<BlockValue> values = new ArrayList<>();
    List<Double> weights = new ArrayList<>();
    double total = 0;
    for (String token : tokens) {
      String[] part = token.split("%", -1);
      if (part.length > 2) throw new IllegalArgumentException("Use weight%block patterns.");
      double weight = part.length == 2 ? number(part[0], 0.0001, 100000, "weight") : 1;
      BlockValue b = block(part[part.length - 1]);
      values.add(b);
      total += weight;
      weights.add(total);
    }
    final double sum = total;
    final long salt = new Random().nextLong();
    return p -> {
      long h =
          salt
              ^ (long) p.x() * 341873128712L
              ^ (long) p.y() * 42317861L
              ^ (long) p.z() * 132897987541L;
      h = (h ^ (h >>> 33)) * 0xff51afd7ed558ccdL;
      double r = (h >>> 11) * 0x1.0p-53 * sum;
      for (int i = 0; i < weights.size(); i++) if (r < weights.get(i)) return values.get(i);
      return values.get(values.size() - 1);
    };
  }

  public Predicate<BlockValue> mask(String text) {
    boolean invert = text.startsWith("!");
    if (invert) text = text.substring(1);
    Predicate<BlockValue> result = b -> false;
    for (String token : text.split(",", -1)) {
      if (token.equals("#existing")) {
        result = result.or(b -> b.id != 0);
        continue;
      }
      if (token.equals("*")) {
        result = b -> true;
        continue;
      }
      BlockValue v = block(token);
      boolean exact =
          token.replace("minecraft:", "").contains(":")
              || defaultMeta.containsKey(token.replace("minecraft:", ""));
      result = result.or(b -> b.id == v.id && (!exact || b.meta == v.meta));
    }
    return invert ? result.negate() : result;
  }

  public static int integer(String s, int min, int max, String label) {
    try {
      int v = Integer.parseInt(s);
      if (v >= min && v <= max) return v;
    } catch (NumberFormatException ignored) {
    }
    throw new IllegalArgumentException(label + " must be " + min + " to " + max + ".");
  }

  public static double number(String s, double min, double max, String label) {
    try {
      double v = Double.parseDouble(s);
      if (Double.isFinite(v) && v >= min && v <= max) return v;
    } catch (NumberFormatException ignored) {
    }
    throw new IllegalArgumentException("Invalid " + label + ".");
  }
}
