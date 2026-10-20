package local.luke.power.input;

/** A visit hides the initial head overlap only until the viewpoint first leaves it. */
public final class HeadReveal {
  private boolean pending;
  public void begin() { pending = true; }
  public void end() { pending = false; }
  public boolean hidden() { return pending; }
  public void update(double dx, double dy, double dz, float yaw, float pitch) {
    if (!pending) return;
    double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
    double x = dx * Math.cos(y) + dz * Math.sin(y);
    double z = -dx * Math.sin(y) + dz * Math.cos(y);
    double vertical = dy * Math.cos(p) + z * Math.sin(p);
    double depth = z * Math.cos(p) - dy * Math.sin(p);
    double half = 4.5 / 16 * HeadGeometry.SCALE;
    pending = Math.abs(x) <= half && Math.abs(depth) <= half
        && vertical >= -.5 / 16 * HeadGeometry.SCALE
        && vertical <= 8.5 / 16 * HeadGeometry.SCALE;
  }
}
