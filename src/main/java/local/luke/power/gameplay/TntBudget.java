package local.luke.power.gameplay;

import java.util.*;
import java.util.function.*;

/** Fair bounded queue. Work stays in its native entity, including its saved fuse. */
public final class TntBudget<T> {
  private final ArrayDeque<T> queue = new ArrayDeque<>();
  private final IdentityHashMap<T, Long> seen = new IdentityHashMap<>();
  private long tick;
  public void begin() { tick++; }
  public void offer(T value) {
    if (!seen.containsKey(value)) queue.addLast(value);
    seen.put(value, tick);
  }
  public void clear() { queue.clear(); seen.clear(); }
  public int size() { return queue.size(); }
  public void run(int maxTicks, int maxExplosions, long nanos, Predicate<T> alive,
      Predicate<T> due, Consumer<T> action, LongSupplier clock) {
    long start = clock.getAsLong();
    int visits = queue.size(), ticks = 0, explosions = 0;
    while (visits-- > 0 && ticks < maxTicks && (ticks == 0 || clock.getAsLong() - start < nanos)) {
      T value = queue.removeFirst();
      Long lastSeen = seen.remove(value);
      if (lastSeen == null || lastSeen != tick || !alive.test(value)) continue;
      boolean explode = due.test(value);
      if (explode && explosions >= maxExplosions) { offer(value); continue; }
      action.accept(value);
      ticks++;
      if (explode) explosions++;
      if (alive.test(value)) offer(value);
    }
  }
}
