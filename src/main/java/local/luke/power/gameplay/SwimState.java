package local.luke.power.gameplay;

/** Transient player pose. A blocked exit retains the small box, never sprint permission. */
public final class SwimState {
  public static final float WIDTH = .6f, HEIGHT = .6f, EYE_HEIGHT = .4f;
  public boolean swimming, compact, crouched;
  public boolean lowCeiling;
  boolean inputPosePrepared;
  public float previousAmount, amount;
  public float previousEyeHeight=1.62f, eyeHeight=1.62f;
  private boolean cameraResetPending;
  public double waterDepth;
  public int waterJumpTicks;

  public static double settled(double velocity) { return Math.abs(velocity)<.003 ? 0 : velocity; }

  /** Camera.tick, separately from the physical eye and collision dimensions. */
  public void cameraTick(float target) {
    if (cameraResetPending) {
      resetCamera(target);
      return;
    }
    previousEyeHeight=eyeHeight;
    eyeHeight+=(target-eyeHeight)*.5f;
  }

  public void resetCamera(float height) {
    previousEyeHeight=eyeHeight=height;
    cameraResetPending=false;
  }

  /** Resolve a loaded camera only after the first tick establishes its safe pose. */
  public void deferCameraReset() { cameraResetPending=true; }

  public float cameraEye(float delta) {
    return previousEyeHeight+(eyeHeight-previousEyeHeight)*delta;
  }

  public void animate() {
    previousAmount = amount;
    amount = compact ? Math.min(1, amount + .09f) : Math.max(0, amount - .09f);
  }

  public float amount(float delta) {
    return previousAmount + (amount - previousAmount) * delta;
  }

  /** Player.travel, Java 1.21.1. Look steering precedes water acceleration/drag. */
  public static double steer(double velocity, float pitch, boolean jump, boolean waterAbove) {
    double lookY = -Math.sin(pitch * (Math.PI / 180));
    if (lookY > 0 && !jump && !waterAbove) return velocity;
    return velocity + (lookY - velocity) * (lookY < -.2 ? .085 : .06);
  }
}
