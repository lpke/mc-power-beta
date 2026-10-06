package local.luke.power.light;

import java.util.*;

/** Bounded main-thread scans. Rendering consumes snapshots and never reads the world. */
public final class LightCache {
  public static final int WORK_PER_TICK = 2048, MAX_LABELS = 2048;
  public record Cell(int x, int y, int z, int level) {}
  private record Column(int x, int z) {}
  @FunctionalInterface public interface Sampler { int light(int x, int y, int z); }
  private Object context;
  private int radius, vertical, source, cx, cy, cz, cursor;
  private boolean spawnable;
  private List<Column> columns = List.of();
  private final List<Cell> pending = new ArrayList<>();
  private List<Cell> visible = List.of();

  public List<Cell> cells() { return visible; }

  public void clear() {
    context = null;
    cursor = 0;
    pending.clear();
    visible = List.of();
  }

  public void tick(Object world, int x, int y, int z, LightSettings s, Sampler sampler) {
    if (!s.enabled || world == null) { clear(); return; }
    if (context != world || radius != s.radius || vertical != s.verticalRange
        || source != s.lightSource || spawnable != s.spawnableOnly
        || Math.abs((long)x - cx) > s.radius / 2 || Math.abs((long)z - cz) > s.radius / 2
        || Math.abs((long)y - cy) > s.verticalRange) {
      clear();
      if (columns.isEmpty() || radius != s.radius) {
        List<Column> next = new ArrayList<>();
        for (int dx = -s.radius; dx <= s.radius; dx++)
          for (int dz = -s.radius; dz <= s.radius; dz++)
            if (dx * dx + dz * dz <= s.radius * s.radius) next.add(new Column(dx, dz));
        next.sort(Comparator.comparingInt(c -> c.x * c.x + c.z * c.z));
        columns = List.copyOf(next);
      }
      context = world;
      radius = s.radius;
      vertical = s.verticalRange;
      source = s.lightSource;
      spawnable = s.spawnableOnly;
    }
    if (cursor == 0) { cx = x; cy = y; cz = z; }
    int height = vertical * 2 + 1, total = columns.size() * height;
    int budget = Math.max(256, Math.min(16384, s.checksPerTick));
    int stop = Math.min(total, cursor + budget);
    while (cursor < stop && pending.size() < MAX_LABELS) {
      Column c = columns.get(cursor / height);
      int yi = cursor++ % height;
      int by = cy + (yi % 2 == 0 ? yi / 2 : -(yi + 1) / 2);
      int bx = cx + c.x, bz = cz + c.z;
      // Light is sampled at the feet, one block above this support block.
      if (by < 0 || by > 126 || bx <= -32000000 || bx >= 32000000
          || bz <= -32000000 || bz >= 32000000) continue;
      int level = sampler.light(bx, by, bz);
      if (level >= 0 && level <= 15) pending.add(new Cell(bx, by, bz, level));
    }
    if (cursor == total || pending.size() == MAX_LABELS) {
      visible = List.copyOf(pending);
      pending.clear();
      cursor = 0;
    }
  }
}
