package local.luke.power.commands.util;

import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityRegistry;
import net.minecraft.entity.player.PlayerEntity;

/** Resolves loaded entities only, validating every selector option before returning targets. */
public final class EntityTargets {
  private EntityTargets() {}

  public static PlayerEntity player(SharedCommandSource source) {
    PlayerEntity player = source.getPlayer();
    if (player == null || player.world == null)
      throw new IllegalArgumentException("Open a world first.");
    return player;
  }

  public static String type(Entity entity) {
    if (entity instanceof PlayerEntity) return "player";
    Object id = EntityRegistry.classToId.get(entity.getClass());
    return modern(id == null ? "unknown" : id.toString());
  }

  public static String modern(String id) {
    String key = id.replaceFirst("^minecraft:", "").replace("_", "").toLowerCase(Locale.ROOT);
    return switch (key) {
      case "pigzombie", "zombiepigman" -> "zombified_piglin";
      case "lavaslime" -> "magma_cube";
      case "primedtnt" -> "tnt";
      case "fallingsand" -> "falling_block";
      default ->
          id.replaceFirst("^minecraft:", "")
              .replaceAll("([a-z])([A-Z])", "$1_$2")
              .toLowerCase(Locale.ROOT);
    };
  }

  public static String registryName(String input) {
    String wanted = modern(input);
    for (Object key : EntityRegistry.idToClass.keySet())
      if (modern(key.toString()).equals(wanted)) return key.toString();
    throw new IllegalArgumentException("Unknown Beta entity: " + input);
  }

  public static String name(Entity entity) {
    return entity instanceof PlayerEntity p ? p.name : type(entity);
  }

  public static Entity one(SharedCommandSource source, String token) {
    List<Entity> result = resolve(source, token, false);
    if (result.size() != 1) throw new IllegalArgumentException("Select exactly one entity.");
    return result.get(0);
  }

  public static List<Entity> resolve(
      SharedCommandSource source, String token, boolean playersOnly) {
    PlayerEntity self = player(source);
    List<Entity> loaded = new ArrayList<>();
    loaded.add(self);
    for (Object value : new ArrayList<>(self.world.entities)) {
      if (value instanceof Entity entity && entity != self && !entity.dead) loaded.add(entity);
    }
    if (!token.startsWith("@")) {
      List<Entity> named =
          loaded.stream()
              .filter(
                  e ->
                      e instanceof PlayerEntity p && p.name.equals(token)
                          || !playersOnly && Integer.toString(e.id).equals(token))
              .toList();
      if (named.isEmpty())
        throw new IllegalArgumentException("No loaded player or entity named " + token + ".");
      return named;
    }
    if (!token.matches("@[spare](\\[.*])?"))
      throw new IllegalArgumentException("Invalid target selector: " + token);
    char kind = token.charAt(1);
    Map<String, String> options = new LinkedHashMap<>();
    if (token.length() > 2) {
      for (String part : token.substring(3, token.length() - 1).split(",")) {
        if (part.isEmpty()) continue;
        String[] pair = part.split("=", 2);
        if (pair.length != 2 || pair[1].isEmpty() || options.putIfAbsent(pair[0], pair[1]) != null)
          throw new IllegalArgumentException("Invalid or repeated selector option: " + part);
        if (!Set.of("type", "name", "distance", "limit", "sort", "x", "y", "z", "dx", "dy", "dz")
            .contains(pair[0]))
          throw new IllegalArgumentException("Unsupported Beta selector option: " + pair[0]);
      }
    }
    double x = options.containsKey("x") ? CommandNumbers.number(options.get("x")) : self.x;
    double y =
        options.containsKey("y") ? CommandNumbers.number(options.get("y")) : self.boundingBox.minY;
    double z = options.containsKey("z") ? CommandNumbers.number(options.get("z")) : self.z;
    if (Math.abs(x) >= 30000000 || Math.abs(z) >= 30000000 || Math.abs(y) > 4096)
      throw new IllegalArgumentException("Selector origin is outside safe Beta bounds.");
    double[] distance = range(options.getOrDefault("distance", "0.."));
    String type = options.get("type"), name = options.get("name");
    if (type != null && !modern(type.replaceFirst("^!", "")).equals("player"))
      registryName(type.replaceFirst("^!", ""));
    int limit =
        options.containsKey("limit")
            ? CommandNumbers.integer(options.get("limit"), 1, 100000, "Limit")
            : kind == 's' || kind == 'p' || kind == 'r' ? 1 : Integer.MAX_VALUE;
    String sort =
        options.getOrDefault(
            "sort", kind == 'p' ? "nearest" : kind == 'r' ? "random" : "arbitrary");
    if (!Set.of("nearest", "furthest", "random", "arbitrary").contains(sort))
      throw new IllegalArgumentException("Unknown selector sort: " + sort);
    double[] delta = {Double.NaN, Double.NaN, Double.NaN};
    for (int i = 0; i < 3; i++)
      if (options.containsKey(new String[] {"dx", "dy", "dz"}[i]))
        delta[i] = CommandNumbers.number(options.get(new String[] {"dx", "dy", "dz"}[i]));
    List<Entity> result = new ArrayList<>();
    for (Entity entity : loaded) {
      if (kind == 's' && entity != self
          || (playersOnly || kind == 'a' || kind == 'p' || kind == 'r')
              && !(entity instanceof PlayerEntity)) continue;
      if (type != null && !matches(type, type(entity), true)
          || name != null && !matches(name, name(entity), false)) continue;
      double squared = distanceSquared(entity, x, y, z);
      if (squared < distance[0] * distance[0] || squared > distance[1] * distance[1]) continue;
      if (!box(entity, x, y, z, delta)) continue;
      result.add(entity);
    }
    if (sort.equals("nearest") || sort.equals("furthest")) {
      Comparator<Entity> order = Comparator.comparingDouble(e -> distanceSquared(e, x, y, z));
      result.sort(sort.equals("furthest") ? order.reversed() : order);
    } else if (sort.equals("random")) Collections.shuffle(result);
    if (result.isEmpty())
      throw new IllegalArgumentException(
          playersOnly ? "No players matched." : "No entities matched.");
    return List.copyOf(result.subList(0, Math.min(limit, result.size())));
  }

  private static boolean matches(String filter, String value, boolean type) {
    boolean inverse = filter.startsWith("!");
    String wanted = inverse ? filter.substring(1) : filter;
    return (type ? modern(wanted) : wanted).equals(value) != inverse;
  }

  private static double distanceSquared(Entity e, double x, double y, double z) {
    double dx = e.x - x, dy = e.boundingBox.minY - y, dz = e.z - z;
    return dx * dx + dy * dy + dz * dz;
  }

  private static boolean box(Entity e, double x, double y, double z, double[] d) {
    if (Double.isNaN(d[0]) && Double.isNaN(d[1]) && Double.isNaN(d[2])) return true;
    double[] base = {x, y, z},
        min = {e.boundingBox.minX, e.boundingBox.minY, e.boundingBox.minZ},
        max = {e.boundingBox.maxX, e.boundingBox.maxY, e.boundingBox.maxZ};
    for (int i = 0; i < 3; i++) {
      double delta = Double.isNaN(d[i]) ? 0 : d[i];
      if (max[i] <= base[i] + Math.min(0, delta) || min[i] >= base[i] + Math.max(0, delta) + 1)
        return false;
    }
    return true;
  }

  static double[] range(String value) {
    String[] parts = value.split("\\.\\.", -1);
    if (parts.length > 2) throw new IllegalArgumentException("Invalid distance range.");
    double low = parts[0].isEmpty() ? 0 : CommandNumbers.number(parts[0]);
    double high =
        parts.length == 1
            ? low
            : parts[1].isEmpty() ? Double.POSITIVE_INFINITY : CommandNumbers.number(parts[1]);
    if (low < 0 || high < low) throw new IllegalArgumentException("Invalid distance range.");
    return new double[] {low, high};
  }
}
