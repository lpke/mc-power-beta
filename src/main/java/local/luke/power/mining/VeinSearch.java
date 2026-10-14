package local.luke.power.mining;

import java.util.*;
import java.util.function.Predicate;

/** Resumable breadth-first search. Both queued candidates and successful matches are capped. */
public final class VeinSearch {
  public record Position(int x, int y, int z) {}
  private final Position origin;
  private final int maxBlocks, maxChecks, radius;
  private final Set<Position> seen = new HashSet<>();
  private final ArrayDeque<Position> queue = new ArrayDeque<>();
  private final List<Position> blocks = new ArrayList<>();
  private int checks;
  public VeinSearch(Position origin, int maxBlocks, int maxChecks, int radius) {
    if (maxBlocks < 1 || maxChecks < 1 || radius < 1) throw new IllegalArgumentException("Positive search limits required");
    this.origin = origin; this.maxBlocks = maxBlocks; this.maxChecks = maxChecks; this.radius = radius;
    seen.add(origin); queue.add(origin);
  }
  public boolean complete() { return queue.isEmpty() || blocks.size() >= maxBlocks || checks >= maxChecks; }
  public int checks() { return checks; }
  public List<Position> blocks() { return Collections.unmodifiableList(blocks); }
  public void step(int budget, Predicate<Position> matches) {
    while (budget-- > 0 && !complete()) {
      Position pos = queue.removeFirst(); checks++;
      if (!matches.test(pos)) continue;
      blocks.add(pos);
      if (blocks.size() >= maxBlocks) break;
      for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
        Position next = new Position(pos.x + dx, pos.y + dy, pos.z + dz);
        if (next.y < 0 || next.y >= 128 || Math.abs((long)next.x - origin.x) > radius
            || Math.abs((long)next.y - origin.y) > radius || Math.abs((long)next.z - origin.z) > radius) continue;
        if (seen.size() < maxChecks && seen.add(next)) queue.addLast(next);
      }
    }
  }
}
