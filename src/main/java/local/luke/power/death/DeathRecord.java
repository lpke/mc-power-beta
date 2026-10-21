package local.luke.power.death;

import java.util.*;

/** Plain fields are persisted with the game's Gson 2.8.9. */
public final class DeathRecord {
  public String id, world, player, cause;
  public int x, y, z, dimension;
  public long date;
  public boolean kept;
  /** Zero is the historical layout; two appends offhand at logical slot 45. */
  public int slotLayout;
  public List<String> items = new ArrayList<>();
  /** Main 0..35, armor 36..39, cursor 40, crafting 41..44, offhand 45. */
  public List<Integer> slots = new ArrayList<>();

  void validate() {
    if (id == null || !id.matches("[1-9][0-9]{0,18}") || world == null || world.isBlank()
        || player == null || player.isBlank() || cause == null || cause.length() > 256
        || slotLayout < 0 || slotLayout > 2 || items == null || items.size() > 128 || items.stream().anyMatch(Objects::isNull)
        || slots == null || slots.size() != items.size()
        || (slots.stream().anyMatch(s -> s == null || s < 0 || s > (slotLayout == 2 ? 45 : 44))
            || new HashSet<>(slots).size() != slots.size()))
      throw new IllegalArgumentException("Invalid death record");
  }
}
