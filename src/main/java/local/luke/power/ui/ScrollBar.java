package local.luke.power.ui;

/** Shared thumb geometry and dragging. All positions are logical GUI pixels. */
public final class ScrollBar {
  private boolean dragging;
  private double offset;

  public record Track(int x, int top, int height, int content, double scroll) {
    public double maximum() {
      return Math.max(0, content - height);
    }

    public boolean visible() {
      return height > 0 && maximum() > 0;
    }

    public int thumb() {
      return Math.min(height, Math.max(12, height * height / Math.max(1, content)));
    }

    public double y() {
      return top
          + Math.max(0, Math.min(maximum(), scroll)) * (height - thumb()) / Math.max(1, maximum());
    }

    public double position(double mouseY, double offset) {
      return Math.max(
          0,
          Math.min(maximum(), (mouseY - top - offset) * maximum() / Math.max(1, height - thumb())));
    }
  }

  public boolean press(Track track, int x, int y) {
    if (!track.visible()
        || x < track.x - 2
        || x >= track.x + 6
        || y < track.top
        || y >= track.top + track.height) return false;
    offset = y >= track.y() && y < track.y() + track.thumb() ? y - track.y() : track.thumb() / 2d;
    dragging = true;
    return true;
  }

  public double drag(Track track, int y, boolean held) {
    if (!held || !track.visible()) dragging = false;
    return dragging ? track.position(y, offset) : track.scroll;
  }

  public void release() {
    dragging = false;
  }

  public void render(UiScreen screen, Track track) {
    if (!track.visible()) return;
    screen.scrollbar(track.x, track.top, track.height, (int) track.y(), track.thumb(), dragging);
  }
}
