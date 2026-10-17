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
  public static List<Line> layoutOrdered(int width, ToIntFunction<String> measure, List<String> order, Predicate<String> enabled, double size) {
    return last = arrangeOrdered(pending, width, measure, order, enabled, size);
  }
  public static List<Line> arrangeOrdered(List<Line> input, int width, ToIntFunction<String> measure,
      List<String> order, Predicate<String> enabled, double size) {
    Map<String,Line> left = new LinkedHashMap<>();
    List<Line> result = new ArrayList<>();
    for (Line line : input) {
      if (line.id.isEmpty()) result.add(line);
      else left.putIfAbsent(line.id, line);
    }
    int row = 0;
    for (String id : order) {
      if (id.isEmpty()) { row++; continue; }
      Line line = left.remove(id);
      if (line == null || !enabled.test(id)) continue;
      result.add(place(line, row++, width, measure, size));
    }
    for (Line line : left.values()) if (enabled.test(line.id)) result.add(place(line, row++, width, measure, size));
    return List.copyOf(result);
  }
  private static Line place(Line line, int row, int width, ToIntFunction<String> measure, double size) {
    String text = line.text;
    while (!text.isEmpty() && measure.applyAsInt(text) * size > width - 4) text = text.substring(0, text.length() - 1);
    return new Line(text, 2, (int)Math.round(2 + row * 10 * size), (int)Math.ceil(measure.applyAsInt(text) * size), line.id);
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
