package local.luke.power.input;

/** Orthographic framing in reference units, independent of framebuffer resolution and GUI scale. */
public final class PhotoProjection {
  private static final double REFERENCE_HEIGHT = 1080;
  private PhotoProjection() {}
  public static double height() { return REFERENCE_HEIGHT; }
  public static double width(int physicalWidth, int physicalHeight) {
    return REFERENCE_HEIGHT * Math.max(1, physicalWidth) / Math.max(1, physicalHeight);
  }
  public static double pointerScale(int physicalHeight) {
    return REFERENCE_HEIGHT / Math.max(1, physicalHeight);
  }
}
