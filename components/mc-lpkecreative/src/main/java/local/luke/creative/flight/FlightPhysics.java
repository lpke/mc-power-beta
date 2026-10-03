package local.luke.creative.flight;

/** Modern creative acceleration and drag at 20 ticks per second. */
public final class FlightPhysics {
  public record Motion(double x, double y, double z) {
    public Motion drag() {
      return new Motion(x * .91, y * .6, z * .91);
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
    double factor = Math.max(0, Math.min(5, glide)) / 5.0;
    double x = old.x, y = old.y, z = old.z;
    if (side == 0 && forward == 0) {
      x *= factor;
      z *= factor;
    }
    if (vertical == 0) y *= factor;
    double length = Math.hypot(side, forward);
    if (length > 0) {
      double scale = .98 / Math.max(1, length);
      side *= scale;
      forward *= scale;
      double angle = Math.toRadians(yaw), sin = Math.sin(angle), cos = Math.cos(angle);
      x += (side * cos - forward * sin) * speed * sprint;
      z += (forward * cos + side * sin) * speed * sprint;
    }
    y += vertical * speed * 3;
    return new Motion(snap(x), snap(y), snap(z));
  }

  private static double snap(double n) {
    return Math.abs(n) < .003 ? 0 : n;
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
