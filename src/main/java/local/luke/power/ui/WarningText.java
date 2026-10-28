package local.luke.power.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/** Centered warning copy that stays above the dialog's existing controls. */
final class WarningText {
  private static final int LINE_HEIGHT = 11, GAP = 4, MARGIN = 12;
  record Line(String text, int y, int color) {}

  static void draw(UiScreen screen, int preferredWidth, int bottom, String title,
      String message, int messageColor) {
    int span = Math.max(1, Math.min(preferredWidth, screen.width - MARGIN * 2));
    for (Line line : layout(span, bottom, title, message, messageColor, screen::textWidth))
      screen.centeredText(line.text(), line.y(), line.color());
  }

  static List<Line> layout(int span, int bottom, String title, String message,
      int messageColor, ToIntFunction<String> measure) {
    boolean details = !message.isBlank();
    int available = Math.max(1, (bottom - MARGIN + 3 - (details ? GAP : 0)) / LINE_HEIGHT);
    List<String> heading = limited(wrap(title, span, measure),
        details ? Math.max(1, Math.min(3, available - 1)) : available, span, measure);
    List<String> body = details ? limited(wrap(message, span, measure),
        Math.max(0, available - heading.size()), span, measure) : List.of();
    int height = (heading.size() + body.size()) * LINE_HEIGHT - 3 + (body.isEmpty() ? 0 : GAP);
    int y = Math.max(MARGIN, bottom - height);
    List<Line> lines = new ArrayList<>();
    for (String line : heading) { lines.add(new Line(line, y, 0xffffff)); y += LINE_HEIGHT; }
    if (!body.isEmpty()) y += GAP;
    for (String line : body) { lines.add(new Line(line, y, messageColor)); y += LINE_HEIGHT; }
    return lines;
  }

  static List<String> wrap(String text, int span, ToIntFunction<String> measure) {
    List<String> lines = new ArrayList<>();
    for (String paragraph : text.split("\n", -1)) {
      String line = "";
      for (String word : paragraph.split("\\s+")) {
        if (word.isEmpty()) continue;
        if (!line.isEmpty() && measure.applyAsInt(line + " " + word) > span) {
          lines.add(line); line = "";
        }
        while (measure.applyAsInt(word) > span && word.length() > 1) {
          int end = 1;
          while (end < word.length() && measure.applyAsInt(word.substring(0, end + 1)) <= span) end++;
          lines.add(word.substring(0, end)); word = word.substring(end);
        }
        line += (line.isEmpty() ? "" : " ") + word;
      }
      lines.add(line);
    }
    return lines;
  }

  private static List<String> limited(List<String> lines, int maximum, int span,
      ToIntFunction<String> measure) {
    if (maximum == 0) return List.of();
    if (lines.size() <= maximum) return lines;
    List<String> shown = new ArrayList<>(lines.subList(0, maximum));
    String last = shown.get(maximum - 1);
    while (!last.isEmpty() && measure.applyAsInt(last + "...") > span)
      last = last.substring(0, last.length() - 1);
    shown.set(maximum - 1, last + "...");
    return shown;
  }
}
