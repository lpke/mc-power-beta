package local.luke.power.audio;

/** Java 1.21.1 local-player movement sound gain and step threshold. */
public final class FluidSoundMath {
  private FluidSoundMath() {}
  public static float volume(double vx, double vy, double vz, float multiplier) {
    double squared = vx * vx * (double).2f + vy * vy + vz * vz * (double).2f;
    return Double.isFinite(squared) && squared >= 0 ? Math.min(1, (float)Math.sqrt(squared) * multiplier) : 0;
  }
  public static double distance(double dx, double dz) {
    return distance(dx, 0, dz);
  }
  public static double distance(double dx, double dy, double dz) {
    double amount = (float)Math.sqrt(dx * dx + dy * dy + dz * dz) * .6f;
    return Double.isFinite(amount) ? amount : 0;
  }
  public static double nextStep(double distance) { return Math.floor(distance) + 1; }
  public static String splash(float volume) {
    return volume < .25f ? "power_beta:entity.player.splash" : "power_beta:entity.player.splash.high_speed";
  }
}
