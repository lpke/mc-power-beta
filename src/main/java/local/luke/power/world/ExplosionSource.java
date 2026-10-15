package local.luke.power.world;

import java.util.function.Supplier;

/** Native fireballs pass a null attacker, so carry their origin through construction. */
public final class ExplosionSource {
  private static final ThreadLocal<Boolean> FIREBALL = ThreadLocal.withInitial(() -> false);
  private ExplosionSource() {}
  public static boolean fireball() { return FIREBALL.get(); }
  public static <T> T fireball(Supplier<T> action) {
    boolean previous = FIREBALL.get();
    FIREBALL.set(true);
    try { return action.get(); }
    finally {
      if (previous) FIREBALL.set(true);
      else FIREBALL.remove();
    }
  }
}
