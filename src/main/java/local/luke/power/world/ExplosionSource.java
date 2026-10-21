package local.luke.power.world;

import java.util.function.Supplier;
import net.minecraft.entity.Entity;

/** Native fireballs pass a null attacker, so carry their origin through construction. */
public final class ExplosionSource {
  private static final ThreadLocal<Boolean> FIREBALL = ThreadLocal.withInitial(() -> false);
  private static final ThreadLocal<Entity> ATTACKER = new ThreadLocal<>();
  private ExplosionSource() {}
  public static boolean fireball() { return FIREBALL.get(); }
  public static Entity attacker() { return ATTACKER.get(); }
  public static <T> T fireball(Entity attacker, Supplier<T> action) {
    Entity previous = ATTACKER.get();
    if (attacker == null) ATTACKER.remove(); else ATTACKER.set(attacker);
    try { return fireball(action); }
    finally {
      if (previous == null) ATTACKER.remove(); else ATTACKER.set(previous);
    }
  }
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
