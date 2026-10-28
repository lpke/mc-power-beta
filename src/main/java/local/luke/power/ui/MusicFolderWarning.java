package local.luke.power.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/** Folder identity follows the title; consequences keep their own paragraph. */
final class MusicFolderWarning {
  static final String CONSEQUENCE = "Files and playlist entries stay unchanged. Tracks unavailable from remaining imports cannot play again until reimported.";
  private static final int LINE_HEIGHT = 11, GAP = 4, MARGIN = 12;

  static List<WarningText.Line> layout(int span, int bottom, String path, String error,
      ToIntFunction<String> measure) {
    List<String> detail = WarningText.wrap(error.isEmpty() ? CONSEQUENCE : error, span, measure);
    int pathLines = Math.max(1, (bottom - MARGIN + 3 - GAP) / LINE_HEIGHT - detail.size() - 1);
    List<String> identity = new ArrayList<>(WarningText.wrap(path, span, measure));
    if (identity.size() > pathLines) {
      identity = new ArrayList<>(identity.subList(0, pathLines));
      String last = identity.get(pathLines - 1);
      while (!last.isEmpty() && measure.applyAsInt(last + "...") > span)
        last = last.substring(0, last.length() - 1);
      identity.set(pathLines - 1, last + "...");
    }
    int height = (1 + identity.size() + detail.size()) * LINE_HEIGHT - 3 + GAP;
    int y = Math.max(MARGIN, bottom - height);
    List<WarningText.Line> lines = new ArrayList<>();
    lines.add(new WarningText.Line("Remove folder?", y, 0xffffff));
    y += LINE_HEIGHT;
    for (String line : identity) { lines.add(new WarningText.Line(line, y, 0xaaaaaa)); y += LINE_HEIGHT; }
    y += GAP;
    for (String line : detail) {
      lines.add(new WarningText.Line(line, y, error.isEmpty() ? 0xaaaaaa : 0xff8888));
      y += LINE_HEIGHT;
    }
    return List.copyOf(lines);
  }
}
