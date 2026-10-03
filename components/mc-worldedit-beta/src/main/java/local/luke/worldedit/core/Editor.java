package local.luke.worldedit.core;

import java.util.*;
import java.util.function.*;

/** One session belongs to one world. Commands never address other worlds or players. */
public final class Editor {
  public final EditEngine engine;
  public final BlockParser blocks;
  public final Consumer<String> message;
  public Pos pos1, pos2;
  public Clipboard clipboard;
  public Pos clipboardOrigin;
  public Predicate<BlockValue> mask = b -> true;
  public boolean usePos1;
  public int limit = 65536;

  public Editor(WorldAccess world, BlockParser blocks, Consumer<String> message) {
    this.engine = new EditEngine(world, message);
    this.blocks = blocks;
    this.message = message;
  }

  public Region region() {
    if (pos1 == null || pos2 == null)
      throw new IllegalArgumentException("Select two positions with //wand, //pos1 and //pos2.");
    return Region.between(pos1, pos2);
  }

  public void select(Region r) {
    pos1 = r.min();
    pos2 = r.max();
  }

  public void position(int index, Pos p) {
    if (!p.valid()) throw new IllegalArgumentException("Position is outside Beta's world bounds.");
    if (index == 1) pos1 = p;
    else pos2 = p;
    message.accept(
        "Position "
            + index
            + ": "
            + p
            + (pos1 != null && pos2 != null ? " (" + region().volume() + " blocks)" : ""));
  }

  public Pos placement(Pos player) {
    if (usePos1) {
      if (pos1 == null) throw new IllegalArgumentException("Set position 1 first.");
      return pos1;
    }
    return player;
  }

  public void bounded(Region r) {
    if (r.volume() > Math.max(limit * 16L, 65536L))
      throw new IllegalArgumentException("Selection is too large to scan. Reduce it first.");
  }

  public void edit(Iterable<Pos> positions, Function<Pos, BlockValue> desired) {
    Predicate<BlockValue> activeMask = mask;
    engine.submit(positions, p -> activeMask.test(engine.read(p)) ? desired.apply(p) : null);
  }

  public void regionEdit(String kind, String pattern, String maskText, int thickness) {
    Region r = region();
    bounded(r);
    Function<Pos, BlockValue> paint = blocks.pattern(pattern);
    Predicate<BlockValue> filter = maskText == null ? b -> true : blocks.mask(maskText);
    edit(
        r,
        p -> {
          if (!filter.test(engine.read(p))) return null;
          return switch (kind) {
            case "walls" -> r.wall(p) ? paint.apply(p) : null;
            case "faces" -> r.face(p) ? paint.apply(p) : null;
            case "hollow" ->
                p.x() - r.min().x() < thickness
                        || r.max().x() - p.x() < thickness
                        || p.y() - r.min().y() < thickness
                        || r.max().y() - p.y() < thickness
                        || p.z() - r.min().z() < thickness
                        || r.max().z() - p.z() < thickness
                    ? null
                    : paint.apply(p);
            case "center" ->
                Math.abs(2L * p.x() - r.min().x() - r.max().x()) <= 1
                        && Math.abs(2L * p.y() - r.min().y() - r.max().y()) <= 1
                        && Math.abs(2L * p.z() - r.min().z() - r.max().z()) <= 1
                    ? paint.apply(p)
                    : null;
            default -> paint.apply(p);
          };
        });
  }

  public void copy(Pos origin, boolean cut) {
    Region r = region();
    if (r.volume() > limit) throw new IllegalArgumentException("Clipboard exceeds block limit.");
    Map<Pos, BlockValue> data = new LinkedHashMap<>();
    long[] bytes = {0};
    engine.submit(
        r,
        p -> {
          BlockValue state = engine.read(p);
          bytes[0] += state.bytes() + 48;
          if (bytes[0] > 24L * 1024 * 1024)
            throw new IllegalArgumentException("Clipboard exceeds 24 MiB.");
          data.put(p.subtract(origin), state);
          return cut ? BlockValue.AIR : null;
        },
        () -> {
          clipboard = new Clipboard(data);
          clipboardOrigin = origin;
          message.accept("Copied " + data.size() + " blocks.");
        });
  }

  public void paste(Pos origin, boolean skipAir, boolean original, boolean select) {
    if (clipboard == null) throw new IllegalArgumentException("Clipboard is empty.");
    Pos base = original ? clipboardOrigin : origin;
    Map<Pos, BlockValue> data = new LinkedHashMap<>();
    for (var e : clipboard.blocks().entrySet()) {
      if (skipAir && e.getValue().id == 0) continue;
      Pos p = e.getKey().add(base);
      if (!p.valid()) throw new IllegalArgumentException("Paste exceeds Beta's world bounds.");
      data.put(p, e.getValue());
    }
    if (data.isEmpty())
      throw new IllegalArgumentException("Clipboard contains no matching blocks.");
    Predicate<BlockValue> activeMask = mask;
    engine.submit(
        data.keySet(),
        p -> activeMask.test(engine.read(p)) ? data.get(p) : null,
        () -> {
          if (select) {
            Iterator<Pos> it = data.keySet().iterator();
            Region bounds = Region.between(it.next(), data.keySet().iterator().next());
            while (it.hasNext()) {
              Pos p = it.next();
              bounds =
                  Region.between(
                      new Pos(
                          Math.min(bounds.min().x(), p.x()),
                          Math.min(bounds.min().y(), p.y()),
                          Math.min(bounds.min().z(), p.z())),
                      new Pos(
                          Math.max(bounds.max().x(), p.x()),
                          Math.max(bounds.max().y(), p.y()),
                          Math.max(bounds.max().z(), p.z())));
            }
            select(bounds);
          }
        });
  }

  public void duplicate(int count, Pos delta, boolean move, boolean skipAir, boolean select) {
    Region r = region();
    if (r.volume() * (move ? 2L : count) > limit)
      throw new IllegalArgumentException("Result exceeds block limit.");
    // Snapshot sources before planning destination writes, so overlapping copies are deterministic.
    Map<Pos, BlockValue> source = new LinkedHashMap<>();
    long bytes = 0;
    for (Pos p : r) {
      BlockValue b = engine.read(p);
      source.put(p, b);
      bytes += b.bytes() + 48;
      if (bytes > 24L * 1024 * 1024) throw new IllegalArgumentException("Copy exceeds 24 MiB.");
    }
    Map<Pos, BlockValue> data = new LinkedHashMap<>();
    if (move) for (Pos p : r) data.put(p, BlockValue.AIR);
    for (int n = 1; n <= count; n++)
      for (var e : source.entrySet()) {
        if (skipAir && e.getValue().id == 0) continue;
        Pos p =
            e.getKey()
                .add(
                    Math.multiplyExact(delta.x(), n),
                    Math.multiplyExact(delta.y(), n),
                    Math.multiplyExact(delta.z(), n));
        if (!p.valid()) throw new IllegalArgumentException("Result exceeds Beta's world bounds.");
        data.put(p, e.getValue());
      }
    Predicate<BlockValue> activeMask = mask;
    engine.submit(
        data.keySet(),
        p -> activeMask.test(engine.read(p)) ? data.get(p) : null,
        () -> {
          if (select)
            select(r.shift(new Pos(delta.x() * count, delta.y() * count, delta.z() * count)));
        });
  }

  public void query(String kind, String maskText) {
    Region r = region();
    bounded(r);
    Predicate<BlockValue> filter = maskText == null ? b -> true : blocks.mask(maskText);
    Map<String, Integer> counts = new TreeMap<>();
    int[] total = {0};
    engine.submit(
        r,
        p -> {
          BlockValue b = engine.read(p);
          if (filter.test(b)) {
            total[0]++;
            counts.merge(b.id + ":" + b.meta, 1, Integer::sum);
          }
          return null;
        },
        () -> {
          message.accept("Matched " + total[0] + " of " + r.volume() + " blocks.");
          if (kind.equals("distr"))
            counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(12)
                .forEach(e -> message.accept(e.getKey() + ": " + e.getValue()));
        });
  }
}
