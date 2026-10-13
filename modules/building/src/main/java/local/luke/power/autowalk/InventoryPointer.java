package local.luke.power.autowalk;

/** Separates ordered drag deltas from the visible cursor, including several drags in one poll. */
public final class InventoryPointer {
  private int x, y, rawX, rawY, motionX, motionY;
  private boolean absolute, dragging, restore;

  public void event(int eventX, int eventY, int dx, int dy, boolean grabbed) {
    if (!grabbed) {
      dx = absolute ? eventX - rawX : 0;
      dy = absolute ? eventY - rawY : 0;
      if (!absolute) { x = eventX; y = eventY; }
      rawX = eventX; rawY = eventY; absolute = true;
    }
    if (dragging) { motionX += dx; motionY += dy; }
    else { x += dx; y += dy; }
  }

  public void press() { dragging = true; }
  public void release() { dragging = false; restore = true; }
  public int x() { return x; }
  public int y() { return y; }
  public boolean restore() { return restore; }
  public boolean hasMotion() { return motionX != 0 || motionY != 0; }
  public int takeX() { int value = motionX; motionX = 0; return value; }
  public int takeY() { int value = motionY; motionY = 0; return value; }
  public void batchFinished(boolean grabbed) {
    restore = false;
    // An ungrab/warp changes the next absolute baseline. An unfinished uncaptured
    // press retains its baseline until its release arrives in the next poll.
    if (grabbed || !dragging) absolute = false;
  }
  public void clear() { absolute = dragging = restore = false; motionX = motionY = 0; }
}
