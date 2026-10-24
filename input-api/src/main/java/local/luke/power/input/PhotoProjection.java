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

  /** Largest viewport inside the rotated, finite ground footprint, rather than its empty corners. */
  public static double fittedHeight(double spanX, double spanZ, double tilt, double yaw,
      int physicalWidth, int physicalHeight) {
    double aspect = (double) Math.max(1, physicalWidth) / Math.max(1, physicalHeight);
    double angle = Math.toRadians(yaw);
    double cosine = Math.abs(Math.cos(angle)), sine = Math.abs(Math.sin(angle));
    // A horizontal ground plane has no projected height at zero tilt. Keep that
    // endpoint finite without changing the selected camera angle.
    double vertical = Math.max(0.1, Math.abs(Math.sin(Math.toRadians(tilt))));
    return Math.min(Math.max(1, spanX) / (cosine * aspect + sine / vertical),
        Math.max(1, spanZ) / (sine * aspect + cosine / vertical));
  }

  public static double zoomFactor(double zoom) { return Math.pow(2, zoom - 1.25); }
}
