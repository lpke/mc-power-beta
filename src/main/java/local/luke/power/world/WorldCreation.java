package local.luke.power.world;

/** The creation choice is scoped to construction and never becomes a global preference. */
public final class WorldCreation {
  private static final ThreadLocal<Integer> DIFFICULTY = ThreadLocal.withInitial(() -> WorldDifficulty.DEFAULT);
  private WorldCreation() {}
  public static int difficulty() { return DIFFICULTY.get(); }
  public static void withDifficulty(int difficulty, Runnable create) {
    int previous = DIFFICULTY.get();
    DIFFICULTY.set(WorldDifficulty.checked(difficulty));
    try { create.run(); } finally { DIFFICULTY.set(previous); }
  }
}
