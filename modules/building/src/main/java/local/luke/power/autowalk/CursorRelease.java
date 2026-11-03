package local.luke.power.autowalk;

/** Orders a native cursor restoration after older X motion and edge-warp markers. */
public final class CursorRelease {
  private long issued, handled, releaseMarker;
  private int x, y;
  private boolean grabbed;

  public void grabbed(boolean value) { grabbed = value; }

  /** LWJGL restores its saved cursor before changing the native grab state. */
  public boolean restore(int x, int y) {
    if (!grabbed) return false;
    this.x = x; this.y = y;
    releaseMarker = issued + 1;
    return true;
  }

  public void warpIssued() { issued++; }
  public void warpHandled() { handled++; }
  public boolean pending() { return handled < releaseMarker; }
  public int x() { return x; }
  public int y() { return y; }
}
