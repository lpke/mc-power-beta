package local.luke.power.input;

/** Shared modern flight tuning. Frame motion integrates the same tick recurrence exactly. */
public final class FlightMotion {
  public static final double HORIZONTAL_DRAG = .91, VERTICAL_DRAG = .6;
  public static final double ACCELERATION = .05, INPUT = .98, TICKS = 20;
  public static final double HORIZONTAL_SPEED = ACCELERATION * INPUT * TICKS / (1 - HORIZONTAL_DRAG);
  private FlightMotion() {}
  public record Motion(double x, double y, double z) {}
  public record Step(double velocity, double distance) {}

  public static Motion tick(double x, double y, double z, double side, double forward,
      int vertical, float yaw, double speed, double sprint, double drift) {
    double factor = Math.max(0, Math.min(5, drift)) / 5;
    if (side == 0 && forward == 0) { x *= factor; z *= factor; }
    if (vertical == 0) y *= factor;
    double length = Math.hypot(side, forward);
    if (length > 0) {
      double scale = INPUT / Math.max(1, length);
      side *= scale; forward *= scale;
      double angle = Math.toRadians(yaw), sin = Math.sin(angle), cos = Math.cos(angle);
      x += (side * cos - forward * sin) * speed * sprint;
      z += (forward * cos + side * sin) * speed * sprint;
    }
    y += vertical * speed * sprint * 3;
    return new Motion(snap(x), snap(y), snap(z));
  }

  /** State velocity is post-drag blocks/second; distance includes the native pre-drag move. */
  public static Step frame(double velocity, double input, double speedPercent, double sprint,
      double drift, boolean vertical, boolean moving, double seconds) {
    if (!Double.isFinite(velocity) || !Double.isFinite(input) || !Double.isFinite(speedPercent)
        || !Double.isFinite(sprint) || !Double.isFinite(drift) || !Double.isFinite(seconds))
      return new Step(0, 0);
    if (seconds <= 0) return new Step(velocity, 0);
    double drag = vertical ? VERTICAL_DRAG : HORIZONTAL_DRAG;
    double factor = !moving ? Math.max(0, Math.min(5, drift)) / 5 : 1;
    double decay = drag * factor;
    if (decay == 0) return new Step(0, 0);
    double acceleration = input * ACCELERATION * Math.max(0, speedPercent) / 100
        * Math.max(0, sprint) * (vertical ? 3 : INPUT) * TICKS;
    double target = acceleration / (1 - drag);
    double state = drag * target;
    double response = -Math.expm1(Math.log(decay) * TICKS * seconds);
    return new Step(velocity + (state - velocity) * response,
        target * seconds + factor * (velocity - state) * response / (TICKS * (1 - decay)));
  }

  public static double legacyDrift(double responsePerSecond) {
    if (!Double.isFinite(responsePerSecond) || responsePerSecond < 0 || responsePerSecond > 1000)
      throw new IllegalArgumentException("Freecam drag must be from 0 to 1000.");
    return Math.max(0, Math.min(5, 5 * Math.exp(-responsePerSecond / TICKS) / HORIZONTAL_DRAG));
  }
  private static double snap(double value) { return Math.abs(value) < .003 ? 0 : value; }
}
