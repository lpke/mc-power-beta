package local.luke.power.input;

/** Orthographic framing in reference units, independent of framebuffer resolution and GUI scale. */
public final class PhotoProjection {
  /** A screen that owns the world camera transform while another camera mode stays active. */
  public interface CameraOwner {}

  private static final double REFERENCE_HEIGHT = 1080;
  private static final double INITIAL_COVERAGE = 0.84;
  private PhotoProjection() {}
  public static double height() { return REFERENCE_HEIGHT; }
  public static double width(int physicalWidth, int physicalHeight) {
    return REFERENCE_HEIGHT * Math.max(1, physicalWidth) / Math.max(1, physicalHeight);
  }
  public static double pointerScale(int physicalHeight) {
    return REFERENCE_HEIGHT / Math.max(1, physicalHeight);
  }

  /** Preserve the original manual zoom scale after the opening fit. */
  public static double zoomFactor(double zoom) { return Math.pow(2, zoom); }

  public record InitialView(double zoom, double cameraX, double cameraY) {}

  /** Entry-local projection of existing mesh boxes; never reads terrain or changes a live camera. */
  public static final class TerrainFrame {
    private final double cosYaw, sinYaw, cosTilt, sinTilt;
    private double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
    private double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;

    public TerrainFrame(double tiltDegrees, double yawDegrees) {
      double yaw = Math.toRadians(yawDegrees), tilt = Math.toRadians(tiltDegrees);
      cosYaw = Math.cos(yaw); sinYaw = Math.sin(yaw);
      cosTilt = Math.cos(tilt); sinTilt = Math.sin(tilt);
    }

    /** Bounds must already be relative to the photo camera's native origin. */
    public void include(double lowX, double lowY, double lowZ, double highX, double highY, double highZ) {
      double x = (lowX + highX) / 2, y = (lowY + highY) / 2, z = (lowZ + highZ) / 2;
      double halfX = (highX - lowX) / 2, halfY = (highY - lowY) / 2, halfZ = (highZ - lowZ) / 2;
      double projectedX = cosYaw * x + sinYaw * z;
      double projectedY = cosTilt * y + sinTilt * (sinYaw * x - cosYaw * z);
      double radiusX = Math.abs(cosYaw) * halfX + Math.abs(sinYaw) * halfZ;
      double radiusY = Math.abs(cosTilt) * halfY
          + Math.abs(sinTilt) * (Math.abs(sinYaw) * halfX + Math.abs(cosYaw) * halfZ);
      minX = Math.min(minX, projectedX - radiusX); maxX = Math.max(maxX, projectedX + radiusX);
      minY = Math.min(minY, projectedY - radiusY); maxY = Math.max(maxY, projectedY + radiusY);
    }

    /** Fit and center once; returning values leaves later zoom, pan and rotation entirely manual. */
    public InitialView initialView(int physicalWidth, int physicalHeight) {
      double width = width(physicalWidth, physicalHeight);
      double factor = INITIAL_COVERAGE * Math.min(width / Math.max(1, maxX - minX),
          height() / Math.max(1, maxY - minY));
      double zoom = Math.log(factor) / Math.log(2);
      double cameraX = -(minX + maxX) / (2 * width), cameraY = -(minY + maxY) / (2 * height());
      if (!Double.isFinite(zoom) || !Double.isFinite(cameraX) || !Double.isFinite(cameraY))
        return new InitialView(1.25, 0, 0);
      return new InitialView(Math.max(0.01, Math.min(12, zoom)), cameraX, cameraY);
    }
  }
}
