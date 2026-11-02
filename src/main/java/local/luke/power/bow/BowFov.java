package local.luke.power.bow;

/** Java 26.3 Camera.tickFov, applied to the combined sprint/bow target. */
public final class BowFov {
  private float previous = 1, current = 1, basePrevious = 1, baseCurrent = 1;
  private boolean active;

  public boolean active() { return active; }
  public void reset() { active = false; previous = current = basePrevious = baseCurrent = 1; }

  /** Negative useTicks means release or cancellation. Disabled preferences reset the filter. */
  public void tick(int useTicks, float baseTarget, float basePrevious, float baseCurrent) {
    if (!active && useTicks <= 0) return;
    if (!active) { current = basePrevious; active = true; }
    this.basePrevious = basePrevious; this.baseCurrent = baseCurrent;
    float draw = useTicks < 0 ? 0 : Math.min(useTicks / 20f, 1);
    float target = baseTarget * (1 - draw * draw * .15f);
    previous = current;
    current += (target - current) * .5f;
    current = Math.max(.1f, Math.min(1.5f, current));
    // Float half steps can stall one ULP short of the movement filter's baseline.
    // Keep the official draw/release response until both released samples have settled.
    if (useTicks < 0 && Math.abs(previous - basePrevious) <= Math.ulp(basePrevious)
        && Math.abs(current - baseCurrent) <= Math.ulp(baseCurrent)) {
      previous = basePrevious; current = baseCurrent; active = false;
    }
  }

  /** Replace the already-interpolated sprint contribution, leaving the base FOV intact. */
  public float factor(float delta) {
    return factor(delta, basePrevious + (baseCurrent - basePrevious) * delta);
  }
  /** Cancel the renderer's current movement factor, including movement updates while paused. */
  public float factor(float delta, float renderedBase) {
    if (!active) return 1;
    return (previous + (current - previous) * delta) / renderedBase;
  }
}
