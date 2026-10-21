package local.luke.power.subtitles;

import java.util.*;
import java.util.function.ToIntFunction;
import local.luke.power.status.StatusLayout;

/** The same native-font rectangle and integer rounding serve gameplay and HUD dragging. */
public record SubtitleLayout(double x, double y, double width, double height, double scale,
    int halfWidth, int background, List<Line> lines) {
  public record Line(String text, char arrow, int textX, int textY, int brightness) {}
  public static boolean defaultPosition(SubtitleSettings settings) {
    return settings.position == 8 && settings.offsetX == -1 && settings.offsetY == -30;
  }
  /** Only the default placement yields to visible tweaks. Saved/custom positions stay exact. */
  public SubtitleLayout avoidActiveTweaks(SubtitleSettings settings, StatusLayout.Bounds tweaks) {
    if (!defaultPosition(settings) || tweaks == null
        || width <= 0 || height <= 0 || tweaks.width() <= 0 || tweaks.height() <= 0
        || x >= tweaks.x() + tweaks.width() || x + width <= tweaks.x()
        || y >= tweaks.y() + tweaks.height() || y + height <= tweaks.y()) return this;
    return new SubtitleLayout(x, Math.max(0, tweaks.y() - height - 2), width, height, scale, halfWidth, background, lines);
  }
  public static SubtitleLayout at(SubtitleSettings settings, List<SubtitleState.Visible> visible,
      int width, int height, ToIntFunction<String> measure) {
    int decorations = measure.applyAsInt("<") + measure.applyAsInt(">") + 2 * measure.applyAsInt(" ");
    int maxWidth = Math.max(0, (int)(width / settings.textSize) - decorations - 2);
    int maxRows = Math.max(0, (int)(height / (10 * settings.textSize)));
    List<SubtitleState.Visible> fitted = new ArrayList<>();
    int textWidth = 0;
    for (var row : visible) {
      if (fitted.size() >= maxRows) break;
      String text = row.label();
      while (!text.isEmpty() && measure.applyAsInt(text) > maxWidth) text = text.substring(0, text.length() - 1);
      fitted.add(new SubtitleState.Visible(row.key(), text, row.arrow(), row.brightness()));
      textWidth = Math.max(textWidth, measure.applyAsInt(text));
    }
    int half = (textWidth + decorations) / 2, unscaledWidth = half * 2 + 2, unscaledHeight = fitted.size() * 10;
    double w = unscaledWidth * settings.textSize, h = unscaledHeight * settings.textSize;
    int column = settings.position % 3, row = settings.position / 3;
    double x = (column == 0 ? 0 : column == 1 ? (width - w) / 2 : width - w) + settings.offsetX;
    double y = (row == 0 ? 0 : row == 1 ? (height - h) / 2 : height - h) + settings.offsetY;
    x = Math.max(0, Math.min(Math.max(0, width - w), x)); y = Math.max(0, Math.min(Math.max(0, height - h), y));
    List<Line> lines = new ArrayList<>();
    for (int i = 0; i < fitted.size(); i++) {
      var cue = fitted.get(i); int center = unscaledHeight - 5 - 10 * i;
      lines.add(new Line(cue.label(), cue.arrow(), half + 1 - measure.applyAsInt(cue.label()) / 2, center - 4, cue.brightness()));
    }
    return new SubtitleLayout(x, y, w, h, settings.textSize, half,
        Math.round(settings.backgroundOpacity * 255 / 100f) << 24, List.copyOf(lines));
  }
  public static SubtitleLayout preview(SubtitleSettings settings, int width, int height, ToIntFunction<String> measure) {
    return at(settings, List.of(new SubtitleState.Visible("preview.steps", "Footsteps", '<', 255),
        new SubtitleState.Visible("preview.water", "Water flows", '\0', 210),
        new SubtitleState.Visible("preview.zombie", "Zombie groans", '>', 160)), width, height, measure);
  }
}
