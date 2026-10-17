package local.luke.power.ui;

import java.util.*;
import java.util.function.*;

/** One frame's debug text, including rows supplied by nested and platform mods. */
public final class DebugOverlay {
  public record Line(String text, int x, int y, int width, String id) {
    public Line(String text, int x, int y, int width) { this(text, x, y, width, DebugLines.identify(text, x, y)); }
  }
  private static final List<Line> pending = new ArrayList<>();
  private static List<Line> last = List.of();
  public static boolean capturing;
  private DebugOverlay() {}

  public static void begin() { pending.clear(); last = List.of(); capturing = false; }
  public static void add(String text, int x, int y) { pending.add(new Line(text, x, y, 0)); }
  public static List<Line> lastLayout() { return last; }

  public static List<Line> layout(int screenWidth, ToIntFunction<String> measure) {
    return last = arrange(pending, screenWidth, measure);
  }
  public static List<Line> layoutOrdered(int width, ToIntFunction<String> measure, List<String> left, List<String> right, Predicate<String> enabled, double size) {
    return last = arrangeOrdered(pending, width, measure, left, right, enabled, size);
  }
  public static List<Line> arrangeOrdered(List<Line> input, int width, ToIntFunction<String> measure,
      List<String> order, Predicate<String> enabled, double size) {
    return arrangeOrdered(input, width, measure, order, DebugColumns.defaultRight(), enabled, size);
  }
  public static List<Line> arrangeOrdered(List<Line> input, int width, ToIntFunction<String> measure,
      List<String> leftOrder, List<String> rightOrder, Predicate<String> enabled, double size) {
    Map<String,Line> lines = new LinkedHashMap<>();
    List<Line> result = new ArrayList<>();
    for (Line line : input) {
      if (line.id.isEmpty()) result.add(line);
      else lines.putIfAbsent(line.id, line);
    }
    int[] rows = new int[2];
    List<List<String>> orders = List.of(leftOrder, rightOrder);
    for (int side = 0; side < 2; side++) for (String id : orders.get(side)) {
      if (id.isEmpty()) { rows[side]++; continue; }
      Line line = lines.remove(id);
      if (line != null && enabled.test(id)) result.add(place(line, rows[side]++, side, width, measure, size));
    }
    for (Line line : lines.values()) if (enabled.test(line.id)) {
      DebugLines known = DebugLines.find(line.id);
      int side = known != null ? known.right() ? 1 : 0 : line.x > width / 2 ? 1 : 0;
      result.add(place(line, rows[side]++, side, width, measure, size));
    }
    return List.copyOf(result);
  }
  private static Line place(Line line, int row, int side, int width, ToIntFunction<String> measure, double size) {
    String text = line.text;
    int available = Math.max(1, width / 2 - 6);
    while (!text.isEmpty() && measure.applyAsInt(text) * size > available) text = text.substring(0, text.length() - 1);
    int span = (int)Math.ceil(measure.applyAsInt(text) * size);
    return new Line(text, side == 0 ? 2 : width - span - 2, (int)Math.round(2 + row * 10 * size), span, line.id);
  }

  /** Pure placement also serves editor samples without touching the captured gameplay frame. */
  public static List<Line> arrange(List<Line> inputLines, int screenWidth, ToIntFunction<String> measure) {
    List<Line> placed = new ArrayList<>();
    List<Line> ordered = new ArrayList<>(inputLines);
    ordered.sort(Comparator.comparingInt(Line::y));
    for (Line input : ordered) {
      String text = input.text;
      while (!text.isEmpty() && measure.applyAsInt(text) > screenWidth - 4) text = text.substring(0, text.length() - 1);
      int width = measure.applyAsInt(text), x = Math.max(2, Math.min(input.x, screenWidth - width - 2));
      int y = Math.max(2, input.y);
      boolean moved;
      do {
        moved = false;
        for (Line other : placed) {
          if (x < other.x + other.width + 2 && x + width + 2 > other.x && y < other.y + 9 && y + 9 > other.y) {
            y = other.y + 9; moved = true;
          }
        }
      } while (moved);
      placed.add(new Line(text, x, y, width));
    }
    return List.copyOf(placed);
  }
}
