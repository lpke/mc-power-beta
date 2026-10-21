package local.luke.power.map;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/** Shared live/preview bounds include the solid frame and scaled information below it. */
public final class MinimapGeometry {
  public static final int BORDER = 4;
  public record Layout(int x, int y, int size) {}
  public record Box(int x, int y, int width, int height) {}
  public record Label(String text, double x, double y, double scale) {}
  public record Frame(Layout layout, Box bounds, List<Label> labels) {
    public Frame translated(int dx, int dy) {
      return new Frame(new Layout(layout.x + dx, layout.y + dy, layout.size),
          new Box(bounds.x + dx, bounds.y + dy, bounds.width, bounds.height),
          labels.stream().map(label -> new Label(label.text, label.x + dx, label.y + dy, label.scale)).toList());
    }
  }
  public record Point(double x, double y) {}
  private MinimapGeometry() {}

  /** Only terrain inside the frame owns zoom input, excluding labels and circle corners. */
  public static boolean contains(Layout layout, boolean circular, double x, double y) {
    double dx = x - layout.x, dy = y - layout.y;
    if (dx < 0 || dy < 0 || dx >= layout.size || dy >= layout.size) return false;
    double radius = layout.size / 2d;
    return !circular || (dx - radius) * (dx - radius) + (dy - radius) * (dy - radius) <= radius * radius;
  }
  public static int informationHeight(MapSettings settings) {
    int rows = (settings.coordinates ? 1 : 0) + (settings.biome ? 1 : 0) + (settings.clock ? 1 : 0);
    return rows == 0 ? 0 : BORDER + 2 + (int)Math.ceil(rows * 10 * settings.belowTextSize / 100d);
  }
  public static Layout layout(MapSettings settings, int width, int height, boolean enlarged) {
    int info = informationHeight(settings);
    int size = Math.max(24, Math.min(enlarged ? settings.size * 2 : settings.size,
        Math.min(width - 12, height - 12 - info)));
    int x = settings.corner % 2 == 0 ? settings.offsetX : width - size - settings.offsetX;
    int y = settings.corner < 2 ? settings.offsetY : height - size - info - settings.offsetY;
    if (enlarged) { x = (width - size) / 2; y = (height - size - info) / 2; }
    return new Layout(clamp(x, BORDER, width - size - BORDER),
        clamp(y, BORDER, height - size - Math.max(info, BORDER) - BORDER), size);
  }
  public static Frame frame(MapSettings settings, int width, int height, boolean enlarged,
      List<String> lines, ToIntFunction<String> measure) {
    Layout layout = layout(settings, width, height, enlarged);
    var labels = new ArrayList<Label>();
    double left = layout.x - BORDER, right = layout.x + layout.size + BORDER;
    double y = layout.y + layout.size + BORDER + 2;
    double bottom = layout.y + layout.size + BORDER;
    double requestedScale = settings.belowTextSize / 100d;
    for (String text : lines) {
      int textWidth = measure.applyAsInt(text);
      double scale = textWidth == 0 ? requestedScale : Math.min(requestedScale, Math.max(1, width - 8d) / (textWidth + 1));
      double drawnWidth = (textWidth + 1) * scale;
      double x = Math.max(BORDER, Math.min(width - BORDER - drawnWidth, layout.x + (layout.size - textWidth * scale) / 2));
      labels.add(new Label(text, x, y, scale));
      left = Math.min(left, x); right = Math.max(right, x + drawnWidth);
      bottom = Math.max(bottom, y + 9 * scale);
      y += 10 * requestedScale;
    }
    int l = (int)Math.floor(left), t = layout.y - BORDER;
    return new Frame(layout, new Box(l, t, (int)Math.ceil(right) - l, (int)Math.ceil(bottom) - t), List.copyOf(labels));
  }
  /** Glyph origins stay fractional as the rendered camera heading changes between ticks. */
  public static Point direction(Layout layout, int index, double angle, boolean circular,
      int textWidth, double scale) {
    double a = index * Math.PI / 2 + angle;
    double distance = Math.max(0, layout.size / 2d - 2 - 5 * scale);
    if (!circular) distance /= Math.max(Math.abs(Math.sin(a)), Math.abs(Math.cos(a)));
    return new Point(layout.x + layout.size / 2d + Math.sin(a) * distance - textWidth * scale / 2,
        layout.y + layout.size / 2d - Math.cos(a) * distance - 4 * scale);
  }
  public static int offsetDirection(int corner, boolean horizontal) {
    return horizontal ? corner % 2 == 1 ? -1 : 1 : corner >= 2 ? -1 : 1;
  }
  private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
}
