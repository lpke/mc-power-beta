package local.luke.power.ui;

import java.util.*;
import java.util.function.Predicate;

/** Two validated orders share stable IDs. Empty strings reserve rows in either column. */
public record DebugColumns(List<String> left, List<String> right) {
  public DebugColumns {
    left = DebugOrder.validate(left); right = DebugOrder.validate(right);
    Set<String> seen = new HashSet<>(left);
    for (String id : right) if (!id.isEmpty() && !seen.add(id))
      throw new IllegalArgumentException("An F3 line can appear in only one column.");
  }
  public static List<String> defaultRight() {
    return Arrays.stream(DebugLines.values()).filter(DebugLines::right).map(line -> line.id).toList();
  }
  public List<String> column(int side) { return side == 0 ? left : right; }
  public DebugColumns complete() {
    List<String> a = new ArrayList<>(left), b = new ArrayList<>(right);
    for (var line : DebugLines.values()) if (!a.contains(line.id) && !b.contains(line.id))
      (line.right() ? b : a).add(line.id);
    return new DebugColumns(a, b);
  }
  /** Move an inclusive selection, retaining gaps and every stable provider ID. */
  public DebugColumns moveRange(int from, int first, int last, int to, int row, boolean insert) {
    List<String> source = new ArrayList<>(column(from));
    if (first < 0 || last < first || last >= source.size()) throw new IllegalArgumentException("Select F3 lines to move.");
    if (from == to && row >= first && row <= last + (insert ? 1 : 0)) return this;
    List<String> group = new ArrayList<>(source.subList(first, last + 1));
    List<String> target = from == to ? source : new ArrayList<>(column(to));
    int destination = Math.max(0, row);
    boolean gap = !insert;
    if (gap) for (int i = destination; i < Math.min(target.size(), destination + group.size()); i++)
      if (!target.get(i).isEmpty()) { gap = false; break; }
    if (gap) {
      for (int i = first; i <= last; i++) source.set(i, "");
      while (target.size() < destination + group.size()) {
        if (target.size() >= DebugOrder.MAX_ROWS) throw new IllegalArgumentException("F3 order supports up to 256 rows.");
        target.add("");
      }
      for (int i = 0; i < group.size(); i++) target.set(destination + i, group.get(i));
    } else {
      source.subList(first, last + 1).clear();
      if (from == to && destination > last) destination -= group.size();
      while (target.size() < destination) target.add("");
      target.addAll(destination, group);
    }
    return from == to ? to == 0 ? new DebugColumns(target, right) : new DebugColumns(left, target)
        : from == 0 ? new DebugColumns(source, target) : new DebugColumns(target, source);
  }

  /** Gap boundaries only add useful targets beside a populated row. */
  public static boolean insertion(List<String> rows, int row, int offset, int height) {
    boolean current = row < rows.size() && !rows.get(row).isEmpty();
    if (current) return true;
    if (offset < 3) return row > 0 && row - 1 < rows.size() && !rows.get(row - 1).isEmpty();
    return offset >= height - 3 && row + 1 < rows.size() && !rows.get(row + 1).isEmpty();
  }

  public DebugColumns move(String id, int from, int to, int row, boolean insert, Predicate<String> enabled) {
    List<String> source = column(from), target = column(to);
    if (from == to) target = DebugOrder.move(source, id, row, insert, enabled);
    else {
      if (!source.contains(id) || id.isEmpty()) throw new IllegalArgumentException("Select an F3 line to move.");
      source = new ArrayList<>(source); source.remove(id);
      List<String> next = new ArrayList<>(target);
      List<String> visible = DebugOrder.visible(target, enabled);
      int destination = Math.max(0, Math.min(DebugOrder.MAX_ROWS - 1, row));
      if (destination >= visible.size()) {
        int gaps = destination - visible.size();
        if (next.size() + gaps + 1 > DebugOrder.MAX_ROWS) throw new IllegalArgumentException("F3 order supports up to 256 rows.");
        while (gaps-- > 0) next.add("");
        next.add(id);
      } else {
        int index = 0, shown = 0;
        for (; index < next.size(); index++) if (next.get(index).isEmpty() || enabled.test(next.get(index))) {
          if (shown++ == destination) break;
        }
        if (!insert && next.get(index).isEmpty()) next.set(index, id); else next.add(index, id);
      }
      target = next;
    }
    if (from == to) return to == 0 ? new DebugColumns(target, right) : new DebugColumns(left, target);
    return from == 0 ? new DebugColumns(source, target) : new DebugColumns(target, source);
  }
}
