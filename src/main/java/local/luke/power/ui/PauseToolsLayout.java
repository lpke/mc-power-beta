package local.luke.power.ui;

import java.util.ArrayList;
import java.util.List;

/** One cluster rectangle drives rendering and clicks, with no overlap against menu controls. */
public final class PauseToolsLayout {
  public static final int BUTTON = 20, STEP = 24;
  public static final List<String> POSITIONS = List.of("Above Back to game", "Top left", "Top center", "Top right",
      "Middle left", "Middle right", "Bottom left", "Bottom center", "Bottom right");
  public record Box(int x, int y, int width, int height) {
    public boolean contains(int mx, int my) { return mx >= x && mx < x + width && my >= y && my < y + height; }
    public boolean overlaps(Box other) { return x < other.x + other.width && x + width > other.x && y < other.y + other.height && y + height > other.y; }
  }
  private PauseToolsLayout() {}
  public static Box at(int width, int height, int position, int count, Box back, List<Box> obstacles) {
    int span = Math.max(0, count * STEP - (STEP - BUTTON));
    int x = switch (position) { case 1, 4, 6 -> 8; case 3, 5, 8 -> width - span - 8; default -> (width - span) / 2; };
    int y = switch (position) { case 0 -> back.y - STEP; case 1, 2, 3 -> 8; case 4, 5 -> (height - BUTTON) / 2; default -> height - BUTTON - 8; };
    x = clamp(x, 4, width - span - 4); y = clamp(y, 4, height - BUTTON - 4);
    var preferred = new Box(x, y, span, BUTTON);
    if (count == 0 || free(preferred, obstacles)) return preferred;
    var candidates = new ArrayList<Integer>(); candidates.add(4); candidates.add(height - BUTTON - 4);
    for (var obstacle : obstacles) { candidates.add(obstacle.y - STEP); candidates.add(obstacle.y + obstacle.height + 4); }
    Box result = preferred; int distance = Integer.MAX_VALUE;
    for (int candidate : candidates) {
      var option = new Box(x, clamp(candidate, 4, height - BUTTON - 4), span, BUTTON);
      int delta = Math.abs(option.y - preferred.y);
      if (delta < distance && free(option, obstacles)) { result = option; distance = delta; }
    }
    if (distance == Integer.MAX_VALUE) {
      // A compact menu may leave exactly one button height between occupied rows.
      for (var obstacle : obstacles) for (int candidate : new int[]{obstacle.y - BUTTON, obstacle.y + obstacle.height}) {
        var option = new Box(x, clamp(candidate, 4, height - BUTTON - 4), span, BUTTON);
        int delta = Math.abs(option.y - preferred.y);
        if (delta < distance && free(option, obstacles)) { result = option; distance = delta; }
      }
    }
    return result;
  }
  private static boolean free(Box box, List<Box> obstacles) { return obstacles.stream().noneMatch(box::overlaps); }
  private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
}
