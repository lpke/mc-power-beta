package local.luke.power.fastplace.config;

import java.util.ArrayList;
import java.util.List;
import local.luke.power.fastplace.RestrictionMode;

public final class Settings {
  public enum ListMode {
    NONE,
    BLACKLIST,
    WHITELIST
  }

  public static final class ItemFilter {
    private final int id, damage;

    public ItemFilter(int id, int damage) { this.id = id; this.damage = damage; }
    public int id() { return id; }
    public int damage() { return damage; }

    @Override public boolean equals(Object other) {
      return other instanceof ItemFilter filter && id == filter.id && damage == filter.damage;
    }
    @Override public int hashCode() { return java.util.Objects.hash(id, damage); }

    public boolean matches(int item, int meta) {
      return id == item && (damage < 0 || damage == meta);
    }

    @Override
    public String toString() {
      return damage < 0 ? "" + id : id + ":" + damage;
    }
  }

  public boolean enabled = false;
  public boolean announceToggle, announceRestrictionToggle;
  public int attemptsPerTick = 2;
  public boolean rememberOrientation = true;
  public local.luke.power.fastplace.SlabMode slabMode = local.luke.power.fastplace.SlabMode.DOUBLE;
  public boolean restrictionEnabled = true;
  public boolean restrictionTiedToFast = true;
  public RestrictionMode restrictionMode = RestrictionMode.FACE;
  public ListMode listMode = ListMode.BLACKLIST;
  // Tweakeroo's default exclusions are Ender chests and shulker boxes, absent in Beta.
  public List<ItemFilter> blacklist = List.of();
  public List<ItemFilter> whitelist = List.of();

  public Settings copy() {
    Settings s = new Settings();
    s.enabled = enabled;
    s.announceToggle = announceToggle;
    s.announceRestrictionToggle = announceRestrictionToggle;
    s.attemptsPerTick = attemptsPerTick;
    s.slabMode = slabMode;
    s.rememberOrientation = rememberOrientation;
    s.restrictionEnabled = restrictionEnabled;
    s.restrictionTiedToFast = restrictionTiedToFast;
    s.restrictionMode = restrictionMode;
    s.listMode = listMode;
    s.blacklist = List.copyOf(blacklist);
    s.whitelist = List.copyOf(whitelist);
    return s;
  }

  public void setEnabled(boolean value) {
    enabled = value;
    if (restrictionTiedToFast) restrictionEnabled = value;
  }

  public boolean permits(int itemId, int damage) {
    if (listMode == ListMode.NONE) return true;
    List<ItemFilter> entries = listMode == ListMode.BLACKLIST ? blacklist : whitelist;
    for (ItemFilter entry : entries) {
      if (entry.matches(itemId, damage)) return listMode == ListMode.WHITELIST;
    }
    return listMode == ListMode.BLACKLIST;
  }

  public static List<ItemFilter> parseFilters(String text) {
    if (text.isBlank()) return List.of();
    if (text.length() > 4096) throw new IllegalArgumentException("Item list is too long");
    List<ItemFilter> result = new ArrayList<>();
    for (String token : text.split(",", -1)) {
      String[] parts = token.trim().split(":", -1);
      if (parts.length > 2)
        throw new IllegalArgumentException("Use item IDs, optionally followed by :metadata");
      try {
        int id = Integer.parseInt(parts[0]);
        int damage = parts.length == 2 ? Integer.parseInt(parts[1]) : -1;
        if (id < 0
            || id > 1048575
            || damage < -1
            || damage > 32767
            || (parts.length == 2 && damage < 0)) throw new NumberFormatException();
        ItemFilter filter = new ItemFilter(id, damage);
        if (!result.contains(filter)) result.add(filter);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Use item IDs like 1, 44:2");
      }
    }
    return List.copyOf(result);
  }

  public static String formatFilters(List<ItemFilter> values) {
    return String.join(", ", values.stream().map(ItemFilter::toString).toList());
  }
}
