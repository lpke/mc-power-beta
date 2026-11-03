package local.luke.power.visual;

/** Bounds the rendered cape only; player and vehicle movement remain untouched. */
public final class CapeAngles {
  private CapeAngles() {}
  public static float pitch(float angle) { return Float.isFinite(angle) ? Math.max(0, Math.min(90, angle)) : 0; }
  public static float sideways(float angle) { return Float.isFinite(angle) ? Math.max(-30, Math.min(30, angle)) : 0; }

  /** Minimum trailing angle behind the rendered leg, including native armor's one-pixel dilation. */
  public static float legClearance(float pitch, float pivotY, float pivotZ) {
    if (!Float.isFinite(pitch) || !Float.isFinite(pivotY) || !Float.isFinite(pivotZ)) return 0;
    double sine = Math.sin(pitch), cosine = Math.cos(pitch), slope = 0;
    // Beta's leg is twelve pixels long and four deep. The cape starts two pixels behind the body.
    for (int end = 0; end < 2; end++) for (int side = 0; side < 2; side++) {
      double y = end == 0 ? -1 : 13, z = side == 0 ? -3 : 3;
      double renderedY = pivotY + y * cosine - z * sine;
      double renderedZ = pivotZ + y * sine + z * cosine - 2;
      if (renderedY > 0) slope = Math.max(slope, renderedZ / renderedY);
    }
    return (float)Math.toDegrees(Math.atan(slope));
  }
}
