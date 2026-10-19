package local.luke.power.map;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Consumer;

/** Ready visible updates precede initial fill; missing entries rotate within a bounded queue. */
final class MapRefreshQueue {
  private final LinkedHashSet<MapMath.Key> keys = new LinkedHashSet<>();
  private boolean preferUpdates = true;
  boolean add(MapMath.Key key) { return keys.add(key); }
  int size() { return keys.size(); }
  boolean isEmpty() { return keys.isEmpty(); }
  void clear() { keys.clear(); preferUpdates = true; }
  void requestMissing(Predicate<MapMath.Key> ready, Consumer<MapMath.Key> request) {
    var deferred = new ArrayList<MapMath.Key>(2);
    int scanned = 0;
    for (var key : keys) {
      if (scanned++ >= 64 || deferred.size() >= 2) break;
      if (!ready.test(key)) deferred.add(key);
    }
    keys.removeAll(deferred); keys.addAll(deferred);
    deferred.forEach(request);
  }
  boolean updatesFirst(boolean filling) {
    if (!filling) return true;
    boolean result = preferUpdates; preferUpdates = !preferUpdates; return result;
  }
  MapMath.Key take(Predicate<MapMath.Key> ready, MapProjection view) {
    MapMath.Key best = null;
    double distance = Double.POSITIVE_INFINITY;
    boolean bestVisible = false;
    var deferred = new ArrayList<MapMath.Key>();
    var it = keys.iterator();
    for (int count = 0; count < 64 && it.hasNext(); count++) {
      var key = it.next();
      if (!ready.test(key)) { it.remove(); deferred.add(key); continue; }
      double dx = key.minX() + key.span() / 2d - view.x;
      double dz = key.minZ() + key.span() / 2d - view.z;
      double rank = dx * dx + dz * dz;
      var center = view.screen(key.minX() + key.span() / 2d, key.minZ() + key.span() / 2d);
      double radius = key.span() * view.scale / 2;
      boolean visible = center.x() + radius >= view.left && center.x() - radius <= view.left + view.width
          && center.y() + radius >= view.top && center.y() - radius <= view.top + view.height;
      if (best == null || visible && !bestVisible || visible == bestVisible && rank < distance) {
        best = key; distance = rank; bestVisible = visible;
      }
    }
    keys.addAll(deferred);
    if (best != null) keys.remove(best);
    return best;
  }
}
