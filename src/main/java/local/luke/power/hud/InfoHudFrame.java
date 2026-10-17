package local.luke.power.hud;

import java.util.*;
import java.util.function.ToIntFunction;

/** Shared clipping and anchoring keep editor and gameplay text geometry identical. */
public record InfoHudFrame(List<String> lines, int textWidth, InfoHudLayout layout, double scale, int argb) {
  public static InfoHudFrame at(InfoHudSettings.Cluster cluster, InfoHudData data, int width, int height,
      ToIntFunction<String> measure) {
    int maxWidth = Math.max(1, (int)((width - 8) / cluster.textSize));
    int maxRows = Math.max(0, (int)((height - 8) / (10 * cluster.textSize)));
    List<String> lines = data.lines(cluster).stream().limit(maxRows).map(line -> {
      while (!line.isEmpty() && measure.applyAsInt(line) > maxWidth) line = line.substring(0, line.length() - 1);
      return line;
    }).toList();
    int textWidth = lines.stream().mapToInt(measure).max().orElse(0);
    return new InfoHudFrame(lines, textWidth, InfoHudLayout.at(width, height, textWidth, lines.size(), cluster), cluster.textSize, cluster.argb());
  }
}
