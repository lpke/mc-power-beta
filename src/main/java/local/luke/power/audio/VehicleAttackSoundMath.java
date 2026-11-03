package local.luke.power.audio;

/** Java 26.3 attack charge affects sound selection only, never Beta damage or attack timing. */
public final class VehicleAttackSoundMath {
  private static final double SWEEP_SPEED = (double) .1f * 2.5;
  private VehicleAttackSoundMath() {}

  public static boolean sword(int item) {
    return item == 268 || item == 272 || item == 267 || item == 276 || item == 283;
  }

  public static float delay(int item) {
    if (sword(item)) return 20 / 1.6f;
    return switch (item) {
      case 269, 273, 256, 277, 284, 294, 286, 279 -> 20;
      case 270, 274, 257, 278, 285 -> 20 / 1.2f;
      case 271, 275 -> 20 / .8f;
      case 258 -> 20 / .9f;
      case 290 -> 20;
      case 291 -> 10;
      case 292 -> 20 / 3f;
      default -> 5;
    };
  }

  public static boolean charged(int ticks, int item) {
    return (ticks + .5f) / delay(item) > .9f;
  }

  public static String hit(int ticks, int item, boolean sprinting, boolean grounded, double speedSquared) {
    boolean full = charged(ticks, item);
    if (full && !sprinting && grounded && speedSquared < SWEEP_SPEED * SWEEP_SPEED && sword(item))
      return "entity.player.attack.sweep";
    return full ? "entity.player.attack.strong" : "entity.player.attack.weak";
  }
}
