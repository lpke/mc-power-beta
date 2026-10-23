package local.luke.power.audio;

/** Carries an entity's category through synchronous native positional sound dispatch. */
public final class SoundCategoryScope {
  private static final ThreadLocal<String> CATEGORY = new ThreadLocal<>();
  private SoundCategoryScope() {}
  static String current() { return CATEGORY.get(); }
  public static void run(String category, Runnable sound) {
    String previous = CATEGORY.get();
    CATEGORY.set(category);
    try { sound.run(); }
    finally {
      if (previous == null) CATEGORY.remove(); else CATEGORY.set(previous);
    }
  }
}
