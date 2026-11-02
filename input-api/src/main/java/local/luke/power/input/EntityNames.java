package local.luke.power.input;

import java.util.Locale;

/** Shared modern spellings for native Beta entity registry IDs. */
public final class EntityNames {
  private EntityNames() {}
  public static String modern(String id) {
    String key=id.replaceFirst("^minecraft:","").replace("_","").toLowerCase(Locale.ROOT);
    return switch(key) {
      case "pigzombie","zombiepigman" -> "zombified_piglin";
      case "lavaslime" -> "magma_cube";
      case "primedtnt" -> "tnt";
      case "fallingsand" -> "falling_block";
      default -> id.replaceFirst("^minecraft:","").replaceAll("([a-z])([A-Z])","$1_$2").toLowerCase(Locale.ROOT);
    };
  }
}
