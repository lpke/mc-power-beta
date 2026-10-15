package local.luke.power.input;

/** Actual model-view origin, shared across mappings. Valid only on its render thread. */
public final class RenderedView {
  private static Object world, entity;
  private static Thread thread;
  private static double x, y, z;
  private RenderedView() {}

  public static void clear() { world = entity = null; thread = null; }
  public static boolean valid(Object level, Object camera) {
    return level != null && camera != null && world == level && entity == camera
        && thread == Thread.currentThread();
  }
  public static double x() { return x; }
  public static double y() { return y; }
  public static double z() { return z; }

  public static void capture(Object level, Object camera, float[] m, double baseX, double baseY, double baseZ) {
    clear();
    if (level == null || camera == null || m == null || m.length != 16) return;
    // Invert the affine view transform, including portal scaling and view bobbing.
    double a=m[0], b=m[4], c=m[8], d=m[1], e=m[5], f=m[9], g=m[2], h=m[6], i=m[10];
    double det=a*(e*i-f*h)-b*(d*i-f*g)+c*(d*h-e*g);
    if (!Double.isFinite(det) || Math.abs(det)<1e-12) return;
    double tx=m[12], ty=m[13], tz=m[14];
    double px=baseX-((e*i-f*h)*tx+(c*h-b*i)*ty+(b*f-c*e)*tz)/det;
    double py=baseY-((f*g-d*i)*tx+(a*i-c*g)*ty+(c*d-a*f)*tz)/det;
    double pz=baseZ-((d*h-e*g)*tx+(b*g-a*h)*ty+(a*e-b*d)*tz)/det;
    if (!Double.isFinite(px) || !Double.isFinite(py) || !Double.isFinite(pz)) return;
    x=px; y=py; z=pz; world=level; entity=camera; thread=Thread.currentThread();
  }
}
