package local.luke.power.worldedit.core;

import java.util.*;

/** Deliberately bounded to loaded entities within a sphere around the player. */
public record EntityQuery(Set<String> types, double radius) {
  public static final Set<String> TYPES =
      Set.of(
          "all",
          "items",
          "mobs",
          "hostile",
          "animals",
          "pets",
          "water",
          "projectiles",
          "vehicles",
          "boats",
          "minecarts",
          "tnt",
          "fallingblocks",
          "paintings",
          "creeper",
          "skeleton",
          "spider",
          "giant",
          "zombie",
          "slime",
          "ghast",
          "pigzombie",
          "pig",
          "sheep",
          "cow",
          "chicken",
          "wolf",
          "squid");
  private static final Set<String> HOSTILE =
      Set.of(
          "creeper",
          "skeleton",
          "spider",
          "giant",
          "zombie",
          "slime",
          "ghast",
          "pigzombie",
          "monster");
  private static final Set<String> ANIMALS = Set.of("pig", "sheep", "cow", "chicken", "wolf");

  public EntityQuery {
    types = Set.copyOf(types);
    if (types.isEmpty() || !TYPES.containsAll(types))
      throw new IllegalArgumentException(
          "Unknown entity type. Use items, mobs, hostile, animals, pets, water, projectiles,"
              + " vehicles, tnt, fallingblocks, paintings or all.");
    if (!Double.isFinite(radius) || radius < 1 || radius > 256)
      throw new IllegalArgumentException("Entity radius must be 1 to 256.");
  }

  public static EntityQuery parse(String type, String radius) {
    return new EntityQuery(
        new HashSet<>(Arrays.asList(type.toLowerCase(Locale.ROOT).split(",", -1))),
        BlockParser.number(radius, 1, 256, "entity radius (1 to 256)"));
  }

  public static EntityQuery butcher(Set<Character> flags, String radius) {
    Set<String> types = new HashSet<>(Set.of("hostile"));
    if (flags.contains('a') || flags.contains('f')) types.add("animals");
    if (flags.contains('p') || flags.contains('f')) types.add("pets");
    if (flags.contains('w') || flags.contains('f')) types.add("water");
    return parse(String.join(",", types), radius);
  }

  public boolean contains(double dx, double dy, double dz) {
    return dx * dx + dy * dy + dz * dz <= radius * radius;
  }

  public boolean matches(String name, boolean living, boolean pet) {
    name = name == null ? "" : name.toLowerCase(Locale.ROOT);
    if (pet && !types.contains("pets") && !types.contains("all")) return false;
    if (types.contains("all") || types.contains(name)) return true;
    for (String type : types) {
      boolean match =
          switch (type) {
            case "items" -> name.equals("item");
            case "mobs" -> living;
            case "hostile" -> HOSTILE.contains(name);
            case "animals" -> ANIMALS.contains(name) && !pet;
            case "pets" -> pet;
            case "water" -> name.equals("squid");
            case "projectiles" -> Set.of("arrow", "snowball", "egg", "fireball").contains(name);
            case "vehicles" -> name.equals("boat") || name.equals("minecart");
            case "boats" -> name.equals("boat");
            case "minecarts" -> name.equals("minecart");
            case "tnt" -> name.equals("primedtnt");
            case "fallingblocks" -> name.equals("fallingsand");
            case "paintings" -> name.equals("painting");
            default -> false;
          };
      if (match) return true;
    }
    return false;
  }
}
