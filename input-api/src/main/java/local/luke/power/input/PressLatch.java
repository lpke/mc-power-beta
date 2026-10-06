package local.luke.power.input;

/** Retains a consumed press across display recreation until its release event arrives. */
public final class PressLatch {
  private int held;
  public boolean claim(int key) {
    if (key == 0 || held == key) return false;
    held = key;
    return true;
  }
  public void release(int key) { if (held == key) held = 0; }
}
