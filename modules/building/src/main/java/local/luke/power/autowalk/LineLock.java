package local.luke.power.autowalk;

/** The player's entire width stays inside the block lane selected at activation. */
public final class LineLock {
  private boolean active, constrainX;
  private double lane;
  public void start(double x, double z, float yaw) {
    double radians = Math.toRadians(yaw);
    constrainX = Math.abs(Math.cos(radians)) >= Math.abs(Math.sin(radians));
    lane = Math.floor(constrainX ? x : z);
    active = true;
  }
  public boolean active() { return active; }
  public void stop() { active = false; }
  public double x(double x, double halfWidth, double delta) { return active && constrainX ? clamp(x, halfWidth, delta) : delta; }
  public double z(double z, double halfWidth, double delta) { return active && !constrainX ? clamp(z, halfWidth, delta) : delta; }
  private double clamp(double position, double halfWidth, double delta) {
    double low = Math.min(lane + .5, lane + halfWidth), high = Math.max(lane + .5, lane + 1 - halfWidth);
    // Starting across a lane boundary must not teleport the player into the lane.
    low = Math.min(low, position); high = Math.max(high, position);
    return Math.max(low - position, Math.min(high - position, delta));
  }
}
