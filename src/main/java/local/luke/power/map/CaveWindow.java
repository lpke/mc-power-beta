package local.luke.power.map;

import java.util.function.IntPredicate;

/** Exact camera-height windows keep cave traversal and saved tile identity in agreement. */
public record CaveWindow(int reference, int above, int below) {
  public CaveWindow {
    if (reference < 0 || reference > 127 || above < 0 || above > 32 || below < 1 || below > 64)
      throw new IllegalArgumentException("Invalid cave search window");
  }

  public static CaveWindow at(double reference, int above, int below) {
    if (!Double.isFinite(reference)) throw new IllegalArgumentException("Invalid cave reference");
    return new CaveWindow((int)Math.max(0, Math.min(127, Math.floor(reference))), above, below);
  }

  public int top() { return Math.min(127, reference + above); }
  public int bottom() { return Math.max(0, reference - below); }
  public int layer() { return reference >> 3; }
  public String context() { return "cave-v2/a" + above + "-b" + below + "/p" + (reference & 7); }

  /** A ceiling that fills the window and a window without a floor both report surveyed void. */
  public int floor(IntPredicate solid, IntPredicate visible) {
    int y = descend(top(), bottom(), solid);
    return descend(y, bottom(), py -> !visible.test(py));
  }

  /** Shared by ceiling, air and transparent-body traversal; the lower bound is inclusive. */
  public static int descend(int y, int bottom, IntPredicate skip) {
    while (y >= bottom && skip.test(y)) y--;
    return y;
  }
}
