package local.luke.power.subtitles;

import java.util.*;

/** Modern selection and timing, with explicit caps for bursts of distinct sound positions. */
public final class SubtitleState {
  public static final int MAX_CAPTIONS = 128, MAX_POSITIONS = 64;
  public record Listener(double x, double y, double z, double forwardX, double forwardZ) {
    public boolean valid() { return finite(x, y, z, forwardX, forwardZ); }
  }
  public record Cue(SubtitleCatalog.Caption caption, long time, double x, double y, double z, double range, boolean looping, boolean relative) {}
  public record Visible(String key, String label, char arrow, int brightness) {}
  private record At(long time, double x, double y, double z, boolean relative) {}
  private static final class Row {
    final SubtitleCatalog.Caption caption;
    final double range;
    final boolean looping;
    final ArrayList<At> positions = new ArrayList<>();
    Row(Cue cue) { caption = cue.caption; range = cue.range; looping = cue.looping; }
  }
  private final LinkedHashMap<String, Row> rows = new LinkedHashMap<>();
  private long saturated;
  public void clear() { rows.clear(); saturated = 0; }
  public long saturated() { return saturated; }
  public int positionCount() { return rows.values().stream().mapToInt(row -> row.positions.size()).sum(); }
  public void accept(Cue cue, long now, double multiplier, Listener listener) {
    if (cue == null || cue.caption == null || !finite(cue.x, cue.y, cue.z) || Double.isNaN(cue.range) || cue.range <= 0) return;
    Row row = rows.get(cue.caption.key());
    if (row == null) {
      if (rows.size() >= MAX_CAPTIONS) { saturated++; return; }
      rows.put(cue.caption.key(), row = new Row(cue));
    }
    expire(row, now, multiplier);
    row.positions.removeIf(at -> at.x == cue.x && at.y == cue.y && at.z == cue.z && at.relative == cue.relative);
    if (row.positions.size() == MAX_POSITIONS) {
      int remove = 0;
      for (int i = 1; i < row.positions.size(); i++) {
        At candidate = row.positions.get(i), chosen = row.positions.get(remove);
        boolean outside = listener != null && distance(candidate, listener) >= row.range * row.range;
        boolean oldOutside = listener != null && distance(chosen, listener) >= row.range * row.range;
        if (outside && !oldOutside || outside == oldOutside && candidate.time < chosen.time) remove = i;
      }
      row.positions.remove(remove); saturated++;
    }
    row.positions.add(new At(cue.time, cue.x, cue.y, cue.z, cue.relative));
  }
  public List<Visible> visible(long now, double multiplier, Listener listener) {
    if (listener == null || !listener.valid()) return List.of();
    List<Visible> visible = new ArrayList<>();
    for (Row row : rows.values()) {
      expire(row, now, multiplier); At best = null; double bestDistance = 0;
      for (At at : row.positions) {
        double distance = distance(at, listener);
        if (distance >= row.range * row.range) continue;
        if (best == null || at.time > best.time || at.time == best.time && distance > bestDistance) { best = at; bestDistance = distance; }
      }
      if (best != null) visible.add(new Visible(row.caption.key(), row.caption.label(), best.relative ? '\0' : arrow(listener, best.x, best.y, best.z), brightness(now - best.time, multiplier, row.looping)));
    }
    return List.copyOf(visible);
  }
  public void expire(long now, double multiplier) { rows.values().forEach(row -> expire(row, now, multiplier)); }
  private static void expire(Row row, long now, double multiplier) { row.positions.removeIf(at -> now - at.time > 3000 * multiplier); }
  private static double distance(At at, Listener listener) {
    if (at.relative) return 0;
    double x = at.x - listener.x, y = at.y - listener.y, z = at.z - listener.z; return x*x + y*y + z*z;
  }
  public static int brightness(long age, double multiplier, boolean looping) {
    int hold = looping ? 500 : 0;
    double fraction = Math.max(0, Math.min(1, Math.max(0, age - hold) / ((3000 - hold) * multiplier)));
    return (int)Math.floor(255 - 180 * fraction);
  }
  public static char arrow(Listener listener, double x, double y, double z) {
    x -= listener.x; y -= listener.y; z -= listener.z;
    double length = Math.sqrt(x*x + y*y + z*z); if (length < 1e-4) return '\0';
    double ahead = (x * listener.forwardX + z * listener.forwardZ) / length;
    double side = (x * -listener.forwardZ + z * listener.forwardX) / length;
    return ahead > .5 ? '\0' : side > 0 ? '>' : side < 0 ? '<' : '\0';
  }
  private static boolean finite(double... values) { for (double value : values) if (!Double.isFinite(value)) return false; return true; }
}
