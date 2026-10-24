package local.luke.power.input;

/** Orthographic framing in reference units, independent of framebuffer resolution and GUI scale. */
public final class PhotoProjection {
  /** A screen that owns the world camera transform while another camera mode stays active. */
  public interface CameraOwner {}

  private static final double REFERENCE_HEIGHT = 1080;
  private PhotoProjection() {}
  public static double height() { return REFERENCE_HEIGHT; }
  public static double width(int physicalWidth, int physicalHeight) {
    return REFERENCE_HEIGHT * Math.max(1, physicalWidth) / Math.max(1, physicalHeight);
  }
  public static double pointerScale(int physicalHeight) {
    return REFERENCE_HEIGHT / Math.max(1, physicalHeight);
  }

  /** Preserve the original zoom scale; terrain bounds never change magnification. */
  public static double zoomFactor(double zoom) { return Math.pow(2, zoom); }
}
