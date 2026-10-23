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
    return tick(x, y, z, side, forward, vertical, yaw, speed, sprint, 5, drift);
  }

  public static Motion tick(double x, double y, double z, double side, double forward,
      int vertical, float yaw, double speed, double sprint, double acceleration, double deceleration) {
    double horizontal = retention(side != 0 || forward != 0 ? acceleration : deceleration);
    double up = retention(vertical != 0 ? acceleration : deceleration);
    x *= horizontal; z *= horizontal; y *= up;
    double length = Math.hypot(side, forward);
    if (length > 0) {
      double scale = INPUT / Math.max(1, length);
      side *= scale; forward *= scale;
      double angle = Math.toRadians(yaw), sin = Math.sin(angle), cos = Math.cos(angle);
      // Compensate input so changing acceleration preserves the terminal speed.
      double gain = gain(horizontal, HORIZONTAL_DRAG);
      x += (side * cos - forward * sin) * speed * sprint * gain;
      z += (forward * cos + side * sin) * speed * sprint * gain;
    }
    y += vertical * speed * sprint * 3 * gain(up, VERTICAL_DRAG);
    return new Motion(snap(x), snap(y), snap(z));
  }

  /** State velocity is post-drag blocks/second; distance includes the native pre-drag move. */
  public static Step frame(double velocity, double input, double speedPercent, double sprint,
      double drift, boolean vertical, boolean moving, double seconds) {
    return frame(velocity, input, speedPercent, sprint, 5, drift, vertical, moving, seconds);
  }

  public static Step frame(double velocity, double input, double speedPercent, double sprint,
      double acceleration, double deceleration, boolean vertical, boolean moving, double seconds) {
    if (!Double.isFinite(velocity) || !Double.isFinite(input) || !Double.isFinite(speedPercent)
        || !Double.isFinite(sprint) || !Double.isFinite(acceleration)
        || !Double.isFinite(deceleration) || !Double.isFinite(seconds))
      return new Step(0, 0);
    if (seconds <= 0) return new Step(velocity, 0);
    double drag = vertical ? VERTICAL_DRAG : HORIZONTAL_DRAG;
    double factor = retention(moving ? acceleration : deceleration);
    double decay = drag * factor;
    double inputAcceleration = input * ACCELERATION * Math.max(0, speedPercent) / 100
        * Math.max(0, sprint) * (vertical ? 3 : INPUT) * TICKS;
    double target = inputAcceleration / (1 - drag);
    double state = drag * target;
    if (decay == 0) return new Step(state, target * seconds);
    double response = -Math.expm1(Math.log(decay) * TICKS * seconds);
    return new Step(velocity + (state - velocity) * response,
        target * seconds + factor * (velocity - state) * response / (TICKS * (1 - decay)));
  }

  private static double retention(double level) {
    return Double.isFinite(level) ? Math.max(0, Math.min(5, level)) / 5 : 1;
  }
  private static double gain(double retention, double drag) {
    return (1 - drag * retention) / (1 - drag);
  }

  private static double snap(double value) { return Math.abs(value) < .003 ? 0 : value; }
}
