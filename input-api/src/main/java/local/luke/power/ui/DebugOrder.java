package local.luke.power.ui;

import com.google.gson.*;
import java.util.*;
import java.util.function.Predicate;

/** Empty strings are gap rows. Unknown stable IDs survive edits and older clients. */
public final class DebugOrder {
  public static final int MAX_ROWS = 256;
  private DebugOrder() {}
  public static List<String> defaults() {
    List<String> rows = new ArrayList<>();
    for (var line : DebugLines.values()) {
      if (line.right()) continue;
      if (line == DebugLines.X || line == DebugLines.SEED || line == DebugLines.CULL_TIME) rows.add("");
      rows.add(line.id);
    }
    return List.copyOf(rows);
  }
  public static List<String> validate(List<String> rows) {
    if (rows == null || rows.size() > MAX_ROWS) throw new IllegalArgumentException("F3 order supports up to " + MAX_ROWS + " rows.");
    Set<String> seen = new HashSet<>();
    for (String id : rows) {
      if (id == null || !id.matches("[A-Za-z0-9_.-]{0,96}") || !id.isEmpty() && !seen.add(id))
        throw new IllegalArgumentException("Invalid or duplicate F3 line ID. Original settings preserved.");
    }
    return List.copyOf(rows);
  }
  public static List<String> read(JsonElement value) {
    if (!value.isJsonArray()) throw new IllegalArgumentException("F3 order must be a list.");
    List<String> rows = new ArrayList<>();
    for (var id : value.getAsJsonArray()) {
      if (!id.isJsonPrimitive() || !id.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("F3 line IDs must be text.");
      rows.add(id.getAsString());
    }
    return validate(rows);
  }
  public static List<String> complete(List<String> rows) {
    List<String> result = new ArrayList<>(validate(rows));
    for (var line : DebugLines.values()) if (!line.right() && !result.contains(line.id)) result.add(line.id);
    return validate(result);
  }
  public static List<String> visible(List<String> rows, Predicate<String> enabled) {
    return rows.stream().filter(id -> id.isEmpty() || enabled.test(id)).toList();
  }
  /** A center drop on a gap replaces it; an edge drop inserts a new row. */
  public static List<String> move(List<String> source, String id, int row, boolean insert, Predicate<String> enabled) {
    if (id.isEmpty() || !source.contains(id)) throw new IllegalArgumentException("Select an F3 line to move.");
    List<String> next = new ArrayList<>(source);
    int originalRow = visible(next, enabled).indexOf(id);
    List<String> original = visible(source, enabled);
    int requested = Math.max(0, Math.min(MAX_ROWS - 1, row));
    if (!insert && (requested >= original.size() || original.get(requested).isEmpty())) {
      next.set(next.indexOf(id), "");
      if (requested >= original.size()) {
        int gaps = requested - original.size();
        if (next.size() + gaps + 1 > MAX_ROWS) throw new IllegalArgumentException("F3 order supports up to " + MAX_ROWS + " rows.");
        while (gaps-- > 0) next.add("");
        next.add(id);
      } else {
        int seen = 0;
        for (int i = 0; i < next.size(); i++) if (next.get(i).isEmpty() || enabled.test(next.get(i))) {
          if (seen++ == requested) { next.set(i, id); break; }
        }
      }
      return validate(next);
    }
    next.remove(id);
    int target = Math.max(0, Math.min(MAX_ROWS - 1, row));
    if (target > originalRow) target--;
    List<String> shown = visible(next, enabled);
    int index;
    if (target >= shown.size()) {
      int gaps = target - shown.size();
      if (next.size() + gaps + 1 > MAX_ROWS) throw new IllegalArgumentException("F3 order supports up to " + MAX_ROWS + " rows.");
      while (gaps-- > 0) next.add("");
      index = next.size();
    } else {
      int seen = 0; index = 0;
      for (; index < next.size(); index++) if (next.get(index).isEmpty() || enabled.test(next.get(index))) {
        if (seen++ == target) break;
      }
      if (!insert && next.get(index).isEmpty()) next.remove(index);
    }
    next.add(index, id);
    return validate(next);
  }
}
