package local.luke.power.map;

import java.util.*;
import java.util.function.ToDoubleFunction;

/** A bounded nearest set. Discovery and the refresh of already visible objects are independent. */
final class RadarSelection<T> {
  private record Entry<T>(T value, double distance) {}
  private final IdentityHashMap<T, Entry<T>> selected = new IdentityHashMap<>();
  private final PriorityQueue<Entry<T>> farthest =
      new PriorityQueue<>((a, b) -> Double.compare(b.distance(), a.distance()));
  private int limit;

  void refresh(int limit, ToDoubleFunction<T> distance) {
    this.limit = Math.max(0, limit);
    farthest.clear();
    if (this.limit == 0) { selected.clear(); return; }
    var iterator = selected.entrySet().iterator();
    while (iterator.hasNext()) {
      var current = iterator.next();
      double next = distance.applyAsDouble(current.getKey());
      if (!Double.isFinite(next) || next < 0) iterator.remove();
      else {
        var entry = new Entry<>(current.getKey(), next);
        current.setValue(entry);
        farthest.add(entry);
      }
    }
    while (selected.size() > this.limit) selected.remove(farthest.remove().value());
  }

  boolean accepts(double distance) {
    return limit > 0 && Double.isFinite(distance) && distance >= 0
        && (selected.size() < limit || distance < farthest.element().distance());
  }

  void offer(T value, double distance) {
    if (selected.containsKey(value) || !accepts(distance)) return;
    if (selected.size() == limit) selected.remove(farthest.remove().value());
    var entry = new Entry<>(value, distance);
    selected.put(value, entry);
    farthest.add(entry);
  }

  Collection<T> values() { return selected.keySet(); }
  void clear() { selected.clear(); farthest.clear(); limit = 0; }
}
