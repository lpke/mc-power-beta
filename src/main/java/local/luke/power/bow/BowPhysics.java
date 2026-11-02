package local.luke.power.bow;

/** Unenchanted Java 26.3 bow equations, in blocks and game ticks. */
public final class BowPhysics {
  public static final int DURABILITY = 384;
  public static final double AIR_DRAG = (double).99f, WATER_DRAG = (double).6f, GRAVITY = .05;
  public static final double UNCERTAINTY = .0172275;
  private BowPhysics() {}
  public static float power(float ticks) {
    float value = Math.max(0, ticks) / 20;
    return Math.min(1, (value * value + 2 * value) / 3);
  }
  public static int texture(float ticks) { return ticks >= 18 ? 2 : ticks >= 13 ? 1 : 0; }
  public static int damage(double x, double y, double z) {
    float speed = (float)Math.sqrt(x * x + y * y + z * z);
    return (int)Math.ceil(Math.min(Integer.MAX_VALUE, Math.max(0, speed * 2d)));
  }
  public static int criticalBound(int damage) { return damage / 2 + 2; }
  public static float shotPitch(float random, float power) { return 1 / (random * .4f + 1.2f) + power * .5f; }
  public static double triangle(double first, double second) { return UNCERTAINTY * (first - second); }
}
