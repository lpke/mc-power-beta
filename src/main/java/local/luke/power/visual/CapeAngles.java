package local.luke.power.visual;

/** Bounds the rendered cape only; player and vehicle movement remain untouched. */
public final class CapeAngles {
  private CapeAngles() {}
  public static float pitch(float angle) { return Float.isFinite(angle) ? Math.min(90, angle) : 0; }
  public static float sideways(float angle) { return Float.isFinite(angle) ? Math.max(-30, Math.min(30, angle)) : 0; }
}
