package local.luke.power.building;

public final class ObsidianMining {
  // Beta pickaxes use speed 1 on obsidian, hardness 10. Modern: (9 + 5² + 1) * 1.4 / 50 / 30.
  private static final float VANILLA_PROGRESS = 1.0f / 10 / 30;
  private static final float MODERN_PROGRESS = 49.0f / 50 / 30;

  private ObsidianMining() {}

  /** Scale native progress so water and airborne penalties still apply. */
  public static float progress(float vanilla, int speed) {
    if (speed <= 0) return vanilla;
    float fraction = Math.min(speed, 100) / 100.0f;
    return vanilla * (1 + (MODERN_PROGRESS / VANILLA_PROGRESS - 1) * fraction);
  }
}
