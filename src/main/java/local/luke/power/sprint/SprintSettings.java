package local.luke.power.sprint;

public final class SprintSettings {
  /** Retained legacy data only. World rules own ground-sprint availability. */
  public boolean enabled;
  public int activation, doubleTapTicks = 7;
  public boolean doubleTap = true, jumpBoost = true, particles = true, sprintKnockback = true;
  public double speed = 130;
  public boolean stamina, noSprintInCombat;
  public double capacity = 100,
      drain = 10,
      recharge = 5,
      rechargeStill = 7.5,
      rechargeDelay = 2,
      combatTime = 8,
      restartStamina = 30;
  public double jumpCost;
  public boolean rechargeInCombat;
  public boolean dynamicFov, creativeFov, spectatorFov, freecamFov;
  public double fovStrength = 100;

  public void validate() {
    finite(speed, 100, 200);
    integer(activation, 0, 2);
    integer(doubleTapTicks, 1, 20);
    finite(capacity, 1, 1000);
    finite(drain, .1, 100);
    finite(recharge, .1, 100);
    finite(rechargeStill, .1, 100);
    finite(rechargeDelay, 0, 60);
    finite(combatTime, 0, 120);
    finite(restartStamina, 0, 100);
    finite(jumpCost, 0, 100);
    finite(fovStrength, 0, 100);
  }

  private static void finite(double value, double min, double max) {
    if (!Double.isFinite(value) || value < min || value > max)
      throw new IllegalArgumentException("Sprint value outside " + min + "–" + max);
  }

  private static void integer(int value, int min, int max) {
    if (value < min || value > max)
      throw new IllegalArgumentException("Invalid sprint activation or interval");
  }
}
