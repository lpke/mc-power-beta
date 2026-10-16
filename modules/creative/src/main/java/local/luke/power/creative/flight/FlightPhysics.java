package local.luke.power.creative.flight;

/** Modern creative acceleration and drag at 20 ticks per second. */
public final class FlightPhysics {
  public record Motion(double x, double y, double z) {
    public Motion drag() {
      return new Motion(x * local.luke.power.input.FlightMotion.HORIZONTAL_DRAG,
          y * local.luke.power.input.FlightMotion.VERTICAL_DRAG,
          z * local.luke.power.input.FlightMotion.HORIZONTAL_DRAG);
    }
  }

  private FlightPhysics() {}

  public static Motion step(
      Motion old,
      double side,
      double forward,
      int vertical,
      float yaw,
      double speed,
      double sprint,
      int glide) {
    return step(old, side, forward, vertical, yaw, speed, sprint, 5, glide);
  }

  public static Motion step(Motion old, double side, double forward, int vertical,
      float yaw, double speed, double sprint, double acceleration, double deceleration) {
    var motion = local.luke.power.input.FlightMotion.tick(old.x, old.y, old.z,
        side, forward, vertical, yaw, speed, sprint, acceleration, deceleration);
    return new Motion(motion.x(), motion.y(), motion.z());
  }

  public static float scroll(float speed, int wheel) {
    return scroll(speed, wheel, .005f);
  }

  public static float scroll(float speed, int wheel, float step) {
    if (!Float.isFinite(speed)) speed = .05f;
    int notches = wheel == 0 ? 0 : Integer.signum(wheel) * Math.max(1, Math.abs(wheel / 120));
    return Math.max(0, Math.min(.2f, speed + notches * step));
  }
}
