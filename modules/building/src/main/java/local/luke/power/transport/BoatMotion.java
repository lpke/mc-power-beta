package local.luke.power.transport;

/**
 * Singleplayer movement equations inspected in BTA 7.3_04 and 8.0.1. One reusable state per boat,
 * with no allocations or global entity references per tick. Buoyancy, terrain collisions and drag
 * remain in the game's normal entity tick.
 */
public final class BoatMotion {
  public static final double MAX_SPEED = 0.8;
  public static final double ACCELERATION = 0.0065;
  public double x;
  public double z;
  public float yaw;

  public void step(
      double velocityX,
      double velocityZ,
      float heading,
      float forward,
      float strafe,
      boolean occupied) {
    // Never propagate non-finite motion into chunk coordinates or saved entity data.
    x = Double.isFinite(velocityX) ? velocityX : 0.0;
    z = Double.isFinite(velocityZ) ? velocityZ : 0.0;
    yaw = Float.isFinite(heading) ? heading : 0.0f;
    forward = Float.isFinite(forward) ? Math.max(-1.0f, Math.min(1.0f, forward)) : 0.0f;
    strafe = Float.isFinite(strafe) ? Math.max(-1.0f, Math.min(1.0f, strafe)) : 0.0f;

    if (occupied) {
      double boatAngle = Math.toRadians(yaw + 90.0f);
      double motionAngle = Math.atan2(x, z);
      boolean backwards = Math.cos(-boatAngle - motionAngle) < 0.0;
      double correction = motionAngle + boatAngle + (backwards ? Math.PI : 0.0);
      double oldX = x;
      x = oldX * Math.cos(correction) - z * Math.sin(correction);
      z = oldX * Math.sin(correction) + z * Math.cos(correction);

      double speed = Math.hypot(x, z);
      float rotation =
          (float) (strafe * (3.0 + 2.0 * Math.max((MAX_SPEED - speed * 1.5) / MAX_SPEED, 0.0)));
      float thrust = (float) (forward * ACCELERATION);
      double offsetX = Math.cos(Math.toRadians(yaw)) * thrust;
      double offsetZ = Math.sin(Math.toRadians(yaw)) * thrust;
      yaw += rotation;

      double magnitude = Math.hypot(offsetX, offsetZ);
      double cosine = (x * offsetX + z * offsetZ) / ((speed + 1.0e-11) * (magnitude + 1.0e-11));
      // Roundoff must not turn acos into NaN after long sessions or collisions.
      double multiplier = 1.0 + Math.acos(Math.max(-1.0, Math.min(1.0, cosine))) / Math.PI;
      x += offsetX * multiplier;
      z += offsetZ * multiplier;
    }

    if (!Double.isFinite(x)) x = 0.0;
    if (!Double.isFinite(z)) z = 0.0;
    double speed = Math.hypot(x, z);
    if (speed > MAX_SPEED) {
      x = x / speed * MAX_SPEED;
      z = z / speed * MAX_SPEED;
    }
  }
}
