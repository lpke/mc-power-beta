package local.luke.power.input;

/** Keep a press on its original chord; releasing Ctrl must not activate the plain key. */
public final class PressModifiers {
  private final int[] presses = new int[356];

  public int update(int key, boolean down, int held) {
    int index = key + 100;
    if (index < 0 || index >= presses.length || key == 0) return held;
    if (!down) { presses[index] = 0; return held; }
    if (presses[index] == 0) presses[index] = held + 1;
    return presses[index] - 1;
  }
}
