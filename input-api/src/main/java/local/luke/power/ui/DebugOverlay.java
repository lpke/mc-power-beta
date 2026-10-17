package local.luke.power.ui;

import java.util.*;
import java.util.function.ToIntFunction;

/** One frame's debug text, including rows supplied by nested and platform mods. */
public final class DebugOverlay {
  public record Line(String text, int x, int y, int width) {}
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
