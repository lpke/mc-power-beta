package local.luke.power.map;

/** Short eased zoom, keeping the cursor's world coordinate fixed throughout the transition. */
public final class MapZoom {
  private static final long DURATION = 100_000_000L;
  private long started;
  private double from, target, dx, dz;
  private boolean active;

  public void wheel(MapMath.View view, int direction, double dx, double dz, long now) {
    update(view, now);
    double next =
        MapMath.bounded(
            (active ? target : view.scale) * (direction > 0 ? 1.2 : 1 / 1.2), 1d / 128, 16);
    from = view.scale;
    target = next;
    this.dx = dx;
    this.dz = dz;
    started = now;
    active = from != target;
  }

  public void update(MapMath.View view, long now) {
    if (!active) return;
    double t = Math.max(0, Math.min(1, (double) (now - started) / DURATION));
    double next = from + (target - from) * Math.sin(t * Math.PI / 2);
    view.zoom(next / view.scale, dx, dz);
    if (t >= 1) active = false;
  }

  public void cancel() {
    active = false;
  }
}
