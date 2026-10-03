package local.luke.worldedit.core;

import java.util.*;
import java.util.function.Predicate;

/** Bounded flood scan. Recursive fill travels sideways and down, never above the start. */
public final class FillIterator implements Iterable<Pos>, Iterator<Pos> {
  private final ArrayDeque<Pos> queue = new ArrayDeque<>();
  private final Set<Pos> seen = new HashSet<>();
  private final Pos origin;
  private final double radius2;
  private final int depth, maximum;
  private final boolean recursive;
  private final Predicate<Pos> open;

  public FillIterator(
      Pos origin, double radius, int depth, boolean recursive, int maximum, Predicate<Pos> open) {
    this.origin = origin;
    radius2 = radius * radius;
    this.depth = depth;
    this.recursive = recursive;
    this.maximum = maximum;
    this.open = open;
    queue.add(origin);
    seen.add(origin);
  }

  public Iterator<Pos> iterator() {
    return this;
  }

  public boolean hasNext() {
    return !queue.isEmpty();
  }

  public Pos next() {
    if (!hasNext()) throw new NoSuchElementException();
    Pos p = queue.removeFirst();
    if (open.test(p)) {
      if (recursive || p.y() == origin.y()) {
        offer(p.add(1, 0, 0));
        offer(p.add(-1, 0, 0));
        offer(p.add(0, 0, 1));
        offer(p.add(0, 0, -1));
      }
      offer(p.add(0, -1, 0));
    }
    return p;
  }

  private void offer(Pos p) {
    long dx = (long) p.x() - origin.x(), dz = (long) p.z() - origin.z();
    if (!p.valid()
        || p.y() > origin.y()
        || origin.y() - p.y() >= depth
        || dx * dx + dz * dz > radius2) return;
    if (seen.add(p)) {
      if (seen.size() > maximum) throw new IllegalArgumentException("Fill scan limit exceeded.");
      queue.addLast(p);
    }
  }
}
