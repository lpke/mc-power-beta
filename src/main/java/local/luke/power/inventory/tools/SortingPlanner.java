package local.luke.power.inventory.tools;

import java.util.*;

/** Detached counts and full-data identities. Oversized stacks remain in their original cells. */
public final class SortingPlanner {
  public record Item(String identity, String data, int id, int damage, int count, int limit, boolean stackable) {
    public Item {
      if (identity == null || data == null || count <= 0 || count > 1_000_000 || limit <= 0) throw new IllegalArgumentException("Invalid sorting item");
    }
    Item count(int value) { return new Item(identity, data, id, damage, value, limit, stackable); }
  }
  public record Cell(Item item, int limit, boolean pinned) {
    public Cell { if (limit <= 0) throw new IllegalArgumentException("Invalid sorting slot limit"); }
  }
  public record Deposit(List<Cell> source, List<Cell> destination) {
    public Deposit { source = List.copyOf(source); destination = List.copyOf(destination); }
  }
  private static final Comparator<Item> ORDER = Comparator.comparingInt(Item::id).thenComparingInt(Item::damage).thenComparing(Item::identity);
  private SortingPlanner() {}

  public record Preference(int id, int damage) {
    boolean matches(Item item) { return item.id == id && (damage < 0 || item.damage == damage); }
  }
  public static List<Cell> sort(List<Cell> before) { return sort(before, false, Map.of()); }
  public static List<Cell> sort(List<Cell> before, boolean descending, Map<Integer, Preference> preferences) {
    check(before);
    if (preferences == null || preferences.keySet().stream().anyMatch(slot -> slot == null || slot < 0 || slot >= before.size()))
      throw new IllegalArgumentException("Invalid preferred slot");
    ArrayList<Item> items = new ArrayList<>();
    for (Cell cell : before) if (!cell.pinned && cell.item != null) items.add(cell.item);
    items.sort(descending ? ORDER.reversed() : ORDER);
    ArrayList<Item> groups = new ArrayList<>();
    for (Item item : items) {
      if (!groups.isEmpty()) {
        Item last = groups.get(groups.size() - 1);
        if (item.stackable && last.stackable && item.identity.equals(last.identity) && (long)item.count + last.count <= 1_000_000) {
          groups.set(groups.size() - 1, last.count(last.count + item.count)); continue;
        }
      }
      groups.add(item);
    }
    Map<Integer, Item> preferred = new HashMap<>();
    for (var entry : new TreeMap<>(preferences).entrySet()) {
      Cell cell = before.get(entry.getKey());
      if (cell.pinned || entry.getValue() == null) continue;
      for (int i = 0; i < groups.size(); i++) {
        Item item = groups.get(i);
        if (!entry.getValue().matches(item)) continue;
        int count = item.stackable ? Math.min(item.count, Math.min(item.limit, cell.limit)) : item.count;
        if (count > Math.min(item.limit, cell.limit)) continue;
        preferred.put(entry.getKey(), item.count(count));
        if (count == item.count) groups.remove(i); else groups.set(i, item.count(item.count - count));
        break;
      }
    }
    ArrayList<Cell> result = new ArrayList<>(); int index = 0;
    for (Cell cell : before) {
      if (cell.pinned) { result.add(cell); continue; }
      Item reserved = preferred.get(result.size());
      if (reserved != null) { result.add(new Cell(reserved, cell.limit, false)); continue; }
      if (index >= groups.size()) { result.add(new Cell(null, cell.limit, false)); continue; }
      Item item = groups.get(index); int capacity = Math.min(item.limit, cell.limit);
      int count = item.stackable ? Math.min(item.count, capacity) : item.count;
      if (count > capacity) throw new IllegalArgumentException("A sorted stack does not fit its native slot");
      result.add(new Cell(item.count(count), cell.limit, false));
      if (count == item.count) index++; else groups.set(index, item.count(item.count - count));
    }
    if (index < groups.size()) throw new IllegalArgumentException("Not enough native slots for this sort");
    conservation(before, result); return List.copyOf(result);
  }

  public static Deposit deposit(List<Cell> source, List<Cell> destination) {
    check(source); check(destination);
    Set<String> original = new HashSet<>();
    for (Cell cell : destination) if (cell.item != null) original.add(cell.item.identity);
    ArrayList<Cell> from = new ArrayList<>(source), to = new ArrayList<>(destination);
    for (int i = 0; i < from.size(); i++) {
      Cell cell = from.get(i); Item item = cell.item;
      if (cell.pinned || item == null || !original.contains(item.identity)) continue;
      int remaining = item.count;
      for (boolean empty : new boolean[]{false, true}) for (int j = 0; j < to.size() && remaining > 0; j++) {
        Cell target = to.get(j); Item existing = target.item;
        if (target.pinned || (existing == null) != empty || existing != null && (!item.stackable || !existing.stackable || !existing.identity.equals(item.identity))) continue;
        int capacity = Math.min(item.limit, target.limit) - (existing == null ? 0 : existing.count);
        int move = item.stackable ? Math.min(remaining, Math.max(0, capacity)) : remaining <= capacity ? remaining : 0;
        if (move == 0) continue;
        Item next = existing == null ? item.count(move) : existing.count(existing.count + move);
        to.set(j, new Cell(next, target.limit, false)); remaining -= move;
      }
      from.set(i, new Cell(remaining == 0 ? null : item.count(remaining), cell.limit, false));
    }
    ArrayList<Cell> before = new ArrayList<>(source); before.addAll(destination);
    ArrayList<Cell> after = new ArrayList<>(from); after.addAll(to); conservation(before, after);
    return new Deposit(from, to);
  }
  private static void check(List<Cell> cells) {
    if (cells == null || cells.size() > 128) throw new IllegalArgumentException("Unsupported inventory region");
    for (Cell cell : cells) {
      if (cell == null) throw new IllegalArgumentException("Missing inventory cell");
      if (cell.item != null && cell.item.count > Math.min(cell.limit, cell.item.limit) && !cell.pinned)
        throw new IllegalArgumentException("Oversized native stacks must retain their original cells");
    }
  }
  private static void conservation(List<Cell> before, List<Cell> after) {
    Map<String, Long> totals = new HashMap<>();
    for (Cell cell : before) if (cell.item != null) totals.merge(cell.item.identity, (long)cell.item.count, Long::sum);
    for (Cell cell : after) if (cell.item != null) totals.merge(cell.item.identity, -(long)cell.item.count, Long::sum);
    if (totals.values().stream().anyMatch(count -> count != 0)) throw new IllegalArgumentException("Inventory plan does not conserve native items");
  }
}
