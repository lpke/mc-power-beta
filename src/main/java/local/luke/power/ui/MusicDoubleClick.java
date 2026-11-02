package local.luke.power.ui;

/** A row double-click excludes moved pointers and never pairs different views or tracks. */
final class MusicDoubleClick {
  private String view = "", track = "";
  private long at;
  private int x, y;

  boolean press(String view, String track, int x, int y, long now) {
    boolean twice = this.view.equals(view) && this.track.equals(track) && now >= at
        && now - at <= 350_000_000L && nearby(x, y);
    if (twice) { cancel(); return true; }
    this.view = view; this.track = track; this.x = x; this.y = y; at = now;
    return false;
  }

  void release(int x, int y) { if (!nearby(x, y)) cancel(); }
  private boolean nearby(int x, int y) {
    long dx = (long)x - this.x, dy = (long)y - this.y;
    return dx * dx + dy * dy < 64;
  }
  void cancel() { view = track = ""; at = 0; }
}
