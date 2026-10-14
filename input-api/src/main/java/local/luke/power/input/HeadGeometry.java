package local.luke.power.input;

/** Native player head dimensions, shared by coordinate reporting and the ghost renderer. */
public final class HeadGeometry {
  public static final float SCALE = .9375F;
  public static final double BASE_ABOVE_FEET = (24.0 / 16 + 1.0 / 128) * SCALE;
  public static final double EYE_ABOVE_BASE = (double)1.62F - BASE_ABOVE_FEET;
  private HeadGeometry() {}
  public static double baseY(double eyeY) { return eyeY - EYE_ABOVE_BASE; }
}
