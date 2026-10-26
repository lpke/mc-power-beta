package local.luke.power.ui;

/** Shared slider thumb bounds and pointer conversion in GUI pixels. */
public final class SliderGeometry {
  private SliderGeometry() {}

  public static int thumbWidth(int width) { return Math.min(8, Math.max(0, width)); }

  public static int thumbX(int left, int width, double fraction) {
    return left + (int)Math.round(Math.max(0, Math.min(1, fraction)) * Math.max(0, width - thumbWidth(width)));
  }

  public static double fraction(double pointer, int left, int width) {
    int thumb = thumbWidth(width);
    return Math.max(0, Math.min(1, (pointer - left - thumb / 2d) / Math.max(1, width - thumb)));
  }
}
