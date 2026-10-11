package local.luke.power.death;

import java.util.*;

/** Plain fields are persisted with the game's Gson 2.8.9. */
public final class DeathRecord {
  public String id, world, player, cause;
  public int x, y, z, dimension;
  public long date;
  public List<String> items = new ArrayList<>();

  void validate() {
    if (id == null || !id.matches("[0-9a-z]{1,13}") || world == null || world.isBlank()
        || player == null || player.isBlank() || cause == null || cause.length() > 256
        || items == null || items.size() > 128 || items.stream().anyMatch(Objects::isNull))
      throw new IllegalArgumentException("Invalid death record");
  }
}
