package local.luke.power.render;

/** Interpolates completed simulation steps without advancing physics or the fuse. */
public final class TntMotion {
  public interface Access { TntMotion power$tntMotion(); }
  private boolean initialized;
  private long tick, update;
  private double startX, startY, startZ, x, y, z;
  private int duration = 1;
  public void begin(long tick, double x, double y, double z) {
    this.tick = tick;
    if (!initialized) {
      initialized = true; update = tick;
      startX = this.x = x; startY = this.y = y; startZ = this.z = z;
    }
  }
  public void target(double x, double y, double z) {
    double fraction = fraction(0);
    startX += (this.x - startX) * fraction;
    startY += (this.y - startY) * fraction;
    startZ += (this.z - startZ) * fraction;
    duration = (int)Math.max(1, Math.min(10, tick - update));
    update = tick;
    this.x = x; this.y = y; this.z = z;
  }
  private double fraction(float delta) { return Math.max(0, Math.min(1, (tick - update + (double)delta) / duration)); }
  public double x(float delta) { return startX + (x - startX) * fraction(delta); }
  public double y(float delta) { return startY + (y - startY) * fraction(delta); }
  public double z(float delta) { return startZ + (z - startZ) * fraction(delta); }
  public boolean initialized() { return initialized; }
  public void clear() { initialized = false; duration = 1; }
}
