package local.luke.power.validation;

import java.util.Arrays;

/** Primitive, bounded frame samples. Only the explicit check command sorts or writes results. */
public final class MapPanFrames {
  private static final Recorder RECORDER = new Recorder(1024);
  private MapPanFrames() {}

  public static boolean recording() { return RECORDER.recording; }
  public static void start(Object screen) { RECORDER.start(screen, System.nanoTime()); }
  public static void tick() { RECORDER.tick(); }
  public static void frame(Object screen, double x, double z, boolean dragging) {
    if (RECORDER.recording) RECORDER.frame(screen, x, z, dragging, System.nanoTime());
  }
  static Result finish() { return RECORDER.finish(System.nanoTime()); }
  static String csv() { return RECORDER.csv(); }

  record Result(long elapsedFrames, int storedFrames, double elapsedSeconds, double sampledSeconds,
      double renderFps, int dragFrames, int distinctViewChanges, double dragSeconds, double dragFps,
      double viewChangesPerSecond, double nativeTicksPerSecond, double frameMedianMs, double frameP95Ms,
      double changeMedianMs, double changeP95Ms) {}

  static final class Recorder {
    private final long[] time, ticks;
    private final double[] x, z;
    private final boolean[] drag;
    private Object owner;
    private boolean recording;
    private long start, totalFrames, nativeTicks;
    private int next, size;

    Recorder(int capacity) {
      if (capacity < 2) throw new IllegalArgumentException("At least two samples are required");
      time = new long[capacity]; ticks = new long[capacity]; x = new double[capacity]; z = new double[capacity]; drag = new boolean[capacity];
    }
    void start(Object screen, long now) {
      owner = java.util.Objects.requireNonNull(screen); start = now;
      totalFrames = nativeTicks = 0; next = size = 0; recording = true;
    }
    void tick() { if (recording) nativeTicks++; }
    void frame(Object screen, double viewX, double viewZ, boolean dragging, long now) {
      if (!recording || screen != owner) return;
      time[next] = now; ticks[next] = nativeTicks; x[next] = viewX; z[next] = viewZ; drag[next] = dragging;
      next = (next + 1) % time.length; size = Math.min(time.length, size + 1); totalFrames++;
    }
    private int index(int ordinal) { return Math.floorMod(next - size + ordinal, time.length); }
    Result finish(long now) {
      recording = false;
      if (size < 2) return new Result(totalFrames, size, (now - start) / 1e9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
      long elapsed = time[index(size - 1)] - time[index(0)], dragNanos = 0, lastChange = 0;
      long[] intervals = new long[size - 1], changes = new long[size - 1];
      int frames = 0, dragFrames = 0, viewChanges = 0, changeIntervals = 0;
      for (int i = 1; i < size; i++) {
        int before = index(i - 1), current = index(i); long interval = time[current] - time[before];
        if (interval <= 0) continue;
        intervals[frames++] = interval;
        if (!drag[current] || !drag[before]) { lastChange = 0; continue; }
        dragFrames++; dragNanos += interval;
        if (Math.abs(x[current] - x[before]) > 1e-9 || Math.abs(z[current] - z[before]) > 1e-9) {
          viewChanges++;
          if (lastChange != 0) changes[changeIntervals++] = time[current] - lastChange;
          lastChange = time[current];
        }
      }
      long tickCount = ticks[index(size - 1)] - ticks[index(0)];
      return new Result(totalFrames, size, (now - start) / 1e9, elapsed / 1e9,
          rate(frames, elapsed), dragFrames, viewChanges, dragNanos / 1e9, rate(dragFrames, dragNanos),
          rate(viewChanges, dragNanos), rate(tickCount, elapsed), percentile(intervals, frames, .5),
          percentile(intervals, frames, .95), percentile(changes, changeIntervals, .5), percentile(changes, changeIntervals, .95));
    }
    private static double rate(long count, long nanos) { return nanos <= 0 ? 0 : count * 1e9 / nanos; }
    private static double percentile(long[] values, int count, double fraction) {
      if (count == 0) return 0;
      Arrays.sort(values, 0, count);
      return values[Math.max(0, (int)Math.ceil(count * fraction) - 1)] / 1e6;
    }
    String csv() {
      StringBuilder out = new StringBuilder("nanoTime,nativeTicks,viewX,viewZ,dragging\n");
      for (int i = 0; i < size; i++) {
        int index = index(i);
        out.append(time[index]).append(',').append(ticks[index]).append(',').append(x[index]).append(',')
            .append(z[index]).append(',').append(drag[index]).append('\n');
      }
      return out.toString();
    }
  }
}
