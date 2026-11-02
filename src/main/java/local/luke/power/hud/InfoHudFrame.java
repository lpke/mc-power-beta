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

  /** Live frames retain measured/wrapped text until values, presentation, font or viewport change. */
  public static final class Cache {
    private InfoHudFrame frame;
    private InfoHudData data;
    private Object font;
    private int width, height, position, x, y, opacity, fields;
    private double size;
    private String color;

    public void clear() { frame = null; data = null; font = null; }

    public InfoHudFrame get(InfoHudSettings.Cluster cluster, InfoHudData data, int width, int height,
        Object font, ToIntFunction<String> measure) {
      int fields = InfoHudDemand.fields(cluster);
      if (frame != null && sameData(data, fields) && font == this.font && width == this.width && height == this.height
          && position == cluster.position && x == cluster.offsetX && y == cluster.offsetY
          && opacity == cluster.opacity && size == cluster.textSize && color.equals(cluster.textColor) && fields == this.fields)
        return frame;
      this.data = data; this.font = font; this.width = width; this.height = height;
      position = cluster.position; x = cluster.offsetX; y = cluster.offsetY; opacity = cluster.opacity;
      size = cluster.textSize; color = cluster.textColor; this.fields = fields;
      frame = at(cluster, data, width, height, measure);
      return frame;
    }

    private boolean sameData(InfoHudData next, int fields) {
      return ((fields & InfoHudDemand.COORDINATES) == 0 || next.coordinates().equals(data.coordinates()))
          && ((fields & InfoHudDemand.DIRECTION) == 0 || next.direction().equals(data.direction()))
          && ((fields & InfoHudDemand.BIOME) == 0 || next.biome().equals(data.biome()))
          && ((fields & InfoHudDemand.TIME) == 0 || next.time().equals(data.time()))
          && ((fields & InfoHudDemand.DAY) == 0 || next.day().equals(data.day()))
          && ((fields & InfoHudDemand.CHUNK) == 0 || next.chunk().equals(data.chunk()))
          && ((fields & InfoHudDemand.FPS) == 0 || next.fps().equals(data.fps()));
    }
  }
}
