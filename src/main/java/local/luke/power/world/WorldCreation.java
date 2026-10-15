package local.luke.power.world;

/** The creation choice is scoped to construction and never becomes a global preference. */
public final class WorldCreation {
  private static final ThreadLocal<Integer> DIFFICULTY = ThreadLocal.withInitial(() -> WorldDifficulty.DEFAULT);
  private static final ThreadLocal<local.luke.power.rules.RuleSet> RULES = new ThreadLocal<>();
  private WorldCreation() {}
  public static int difficulty() { return DIFFICULTY.get(); }
  public static local.luke.power.rules.RuleSet rules() {
    return RULES.get() == null ? new local.luke.power.rules.RuleSet() : RULES.get().copy();
  }
  public static void withRules(int difficulty, local.luke.power.rules.RuleSet rules, Runnable create) {
    var previous = RULES.get(); RULES.set(rules.copy());
    try { withDifficulty(difficulty, create); }
    finally { if (previous == null) RULES.remove(); else RULES.set(previous); }
  }
  public static void withDifficulty(int difficulty, Runnable create) {
    int previous = DIFFICULTY.get();
    DIFFICULTY.set(WorldDifficulty.checked(difficulty));
    try { create.run(); } finally { DIFFICULTY.set(previous); }
  }
}
