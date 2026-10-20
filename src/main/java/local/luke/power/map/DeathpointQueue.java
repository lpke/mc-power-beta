package local.luke.power.map;

import java.util.ArrayDeque;

/**
 * Death transitions survive asynchronous waypoint loading with their original time and position.
 */
public final class DeathpointQueue {
  private record Death(int x, int y, int z, int dimension, boolean keepOld, long time, String cause) {}

  private final ArrayDeque<Death> pending = new ArrayDeque<>();
  private int retryTicks;

  public void capture(int x, int y, int z, int dimension, boolean keepOld) {
    if (pending.size() >= WaypointStore.MAX_POINTS)
      throw new IllegalStateException("Too many pending death waypoints");
    pending.addLast(new Death(x, y, z, dimension, keepOld, System.currentTimeMillis(), MapDeathCauses.consume()));
  }

  public int size() {
    return pending.size();
  }

  public void clear() {
    pending.clear();
    retryTicks = 0;
  }

  public void retry() {
    retryTicks = 0;
  }

  public boolean flush(WaypointStore store) {
    if (retryTicks > 0) {
      retryTicks--;
      return false;
    }
    if (pending.isEmpty() || !store.ready()) return false;
    Death death = pending.peekFirst();
    try {
      store.death(death.x, death.y, death.z, death.dimension, death.keepOld, death.time, death.cause);
    } catch (RuntimeException e) {
      retryTicks = 200;
      throw e;
    }
    pending.removeFirst();
    return true;
  }
}
