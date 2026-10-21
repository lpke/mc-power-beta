package local.luke.power.map;

/** Exact, ascending zoom targets. Input always uses the saved target, never an animated scale. */
public final class MinimapZoomSteps {
  private MinimapZoomSteps() {}

  public static void validate(double[] steps) {
    if (steps == null || steps.length < 1 || steps.length > 32)
      throw new IllegalArgumentException("Use 1 to 32 zoom levels.");
    double previous = 0;
    for (double step : steps) {
      if (!Double.isFinite(step) || step < .25 || step > 4)
        throw new IllegalArgumentException("Zoom levels must be between 0.25 and 4.");
      if (step <= previous)
        throw new IllegalArgumentException("Use different zoom levels, from lowest to highest.");
      previous = step;
    }
  }

  public static double next(double[] steps, double target, int direction) {
    validate(steps);
    if (direction > 0) {
      for (double step : steps) if (step > target + 1e-9) return step;
      return steps[0];
    }
    for (int i = steps.length - 1; i >= 0; i--) if (steps[i] < target - 1e-9) return steps[i];
    return steps[steps.length - 1];
  }

  /** A removed/edited default follows the nearest remaining configured step. */
  public static int defaultIndex(double[] steps, double target) {
    validate(steps);
    int nearest = 0;
    for (int i = 1; i < steps.length; i++)
      if (Math.abs(steps[i] - target) < Math.abs(steps[nearest] - target)) nearest = i;
    return nearest;
  }
  public static double reset(double[] steps, double target) { return steps[defaultIndex(steps, target)]; }
}
