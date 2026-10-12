package local.luke.power.gameplay;

import net.minecraft.class_142;
import net.minecraft.class_365;
import net.minecraft.entity.Entity;

/** These exact native classes return null for both entity collision boxes. */
public final class CollisionCandidates {
  public interface ChunkAccess {
    void power$addCollider(Entity entity);
    void power$queryColliders(Entity source, net.minecraft.util.math.Box box, java.util.List<Entity> result, int bottom);
  }
  private CollisionCandidates() {}
  private static final ThreadLocal<Integer> depth = ThreadLocal.withInitial(() -> 0);
  public static boolean simple(Entity entity) {
    return entity != null && (entity.getClass() == class_142.class || entity.getClass() == class_365.class);
  }
  public static boolean active() { return depth.get() > 0; }
  public static void begin() { depth.set(depth.get() + 1); }
  public static void end() { depth.set(depth.get() - 1); }
}
