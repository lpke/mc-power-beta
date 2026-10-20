package local.luke.power.map;

/** Render-only, time-based zoom. Repeated draws at the same time never advance the animation. */
public final class MinimapZoom {
  private static final long DURATION = 180_000_000L;
  private double from, target;
  private long started;
  private boolean initialized;

  public void reset() { initialized = false; }
  public double value(long now) {
    if (!initialized) return 1;
    double t = Math.max(0, Math.min(1, (now - started) / (double)DURATION));
    if (t >= 1) return target;
    return from + (target - from) * (1 - Math.pow(1 - t, 3));
  }
  public double target() { return target; }
  public void set(double next, long now) {
    if (!initialized) { initialized = true; from = target = next; started = now; }
    else if (next != target) { from = value(now); target = next; started = now; }
  }
}
