package local.luke.power.worldedit.core;

import java.util.*;
import java.util.function.*;

/** Plan first, write in bounded batches, retain complete before/after states for undo. */
public final class EditEngine {
  public record Change(Pos pos, BlockValue state) {}

  private record Entry(Pos pos, BlockValue before, BlockValue after) {}

  private record History(List<Entry> entries, long bytes) {}

  private final WorldAccess world;
  private final Consumer<String> report;
  private final Deque<History> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
  private Job job;
  private int limit = 65536, historyLimit = 20;
  private long memoryLimit = 64L * 1024 * 1024;

  private class Job {
    final Iterator<Pos> cursor;
    final Function<Pos, BlockValue> desired;
    final LinkedHashMap<Pos, BlockValue> plan = new LinkedHashMap<>();
    final List<Entry> written = new ArrayList<>();
    final Runnable complete;
    Iterator<Map.Entry<Pos, BlockValue>> apply;
    long scanned, bytes;
    boolean rollback;
    String failure;

    Job(Iterable<Pos> positions, Function<Pos, BlockValue> desired, Runnable complete) {
      cursor = positions.iterator();
      this.desired = desired;
      this.complete = complete;
    }
  }

  public EditEngine(WorldAccess world, Consumer<String> report) {
    this.world = world;
    this.report = report;
  }

  public void configure(int limit, int histories) {
    if (limit < 1 || limit > 262144 || histories < 1 || histories > 100)
      throw new IllegalArgumentException("Invalid edit limits.");
    this.limit = limit;
    historyLimit = histories;
    trim();
  }

  public boolean busy() {
    return job != null;
  }

  public int undoSize() {
    return undo.size();
  }

  public void submit(Iterable<Pos> positions, Function<Pos, BlockValue> desired) {
    submit(positions, desired, () -> {});
  }

  public void submit(
      Iterable<Pos> positions, Function<Pos, BlockValue> desired, Runnable complete) {
    if (busy()) throw new IllegalArgumentException("An edit is running. Use //cancel or wait.");
    job = new Job(positions, desired, complete);
    report.accept("Planning edit...");
  }

  private void check(Pos p) {
    if (!p.valid()) throw new IllegalArgumentException("Edit exceeds Beta's world bounds.");
    if (!world.loaded(p))
      throw new IllegalArgumentException(
          "Edit reaches an unloaded chunk. Move closer or select less.");
  }

  public BlockValue read(Pos p) {
    check(p);
    return world.get(p);
  }

  public void tick(int budget) {
    if (job == null) return;
    Job j = job;
    try {
      for (int i = 0; i < Math.max(1, budget) && job == j; i++) {
        if (j.rollback) {
          if (j.written.isEmpty()) {
            job = null;
            report.accept(j.failure + " Written blocks restored.");
            break;
          }
          Entry e = j.written.remove(j.written.size() - 1);
          world.set(e.pos, e.before);
          continue;
        }
        if (j.apply == null) {
          if (j.cursor.hasNext()) {
            Pos p = j.cursor.next();
            check(p);
            if (++j.scanned > Math.max(limit * 16L, 65536L))
              throw new IllegalArgumentException("Edit scan limit exceeded.");
            BlockValue value = j.desired.apply(p);
            if (value == null) continue;
            // Preflight every existing block entity before making any changes.
            BlockValue before = world.get(p);
            if (value.satisfiedBy(before)) continue;
            BlockValue old = j.plan.put(p, value);
            if (old != null) j.bytes -= old.bytes();
            j.bytes += value.bytes() + before.bytes() + 64;
            if (j.plan.size() > limit)
              throw new IllegalArgumentException("Edit exceeds the " + limit + " block limit.");
            if (j.bytes > memoryLimit / 2)
              throw new IllegalArgumentException(
                  "Edit history is too large; select fewer containers.");
          } else j.apply = j.plan.entrySet().iterator();
        } else if (j.apply.hasNext()) {
          var c = j.apply.next();
          check(c.getKey());
          BlockValue before = world.get(c.getKey());
          // Journal before writing so an adapter failure can also be rolled back.
          j.written.add(new Entry(c.getKey(), before, c.getValue()));
          world.set(c.getKey(), c.getValue());
        } else {
          if (!j.written.isEmpty()) {
            undo.addLast(new History(List.copyOf(j.written), j.bytes));
            redo.clear();
            trim();
          }
          job = null;
          j.complete.run();
          report.accept("Changed " + j.written.size() + " blocks.");
        }
      }
    } catch (RuntimeException e) {
      if (j.rollback) {
        job = null;
        throw new IllegalStateException("Could not restore an interrupted edit", e);
      }
      j.rollback = true;
      j.failure = "Edit stopped: " + e.getMessage();
      if (j.written.isEmpty()) {
        job = null;
        report.accept(j.failure + " No blocks changed.");
      }
    }
  }

  public void cancel() {
    if (job != null) {
      job.rollback = true;
      job.failure = "Edit cancelled.";
    }
  }

  public void finishRollback() {
    cancel();
    while (busy()) tick(4096);
  }

  private void trim() {
    long bytes =
        undo.stream().mapToLong(History::bytes).sum()
            + redo.stream().mapToLong(History::bytes).sum();
    while (!undo.isEmpty() && (undo.size() > historyLimit || bytes > memoryLimit)) {
      bytes -= undo.removeFirst().bytes;
    }
    while (!redo.isEmpty() && (redo.size() > historyLimit || bytes > memoryLimit)) {
      bytes -= redo.removeFirst().bytes;
    }
  }

  public void clearHistory() {
    if (busy()) throw new IllegalArgumentException("Wait for the current edit.");
    undo.clear();
    redo.clear();
  }

  public void history(boolean forward, int count) {
    if (busy()) throw new IllegalArgumentException("Wait for the current edit.");
    Deque<History> source = forward ? redo : undo, target = forward ? undo : redo;
    if (count < 1 || count > source.size())
      throw new IllegalArgumentException(
          "Available " + (forward ? "redo" : "undo") + " steps: " + source.size());
    List<History> selected = new ArrayList<>();
    Iterator<History> it = source.descendingIterator();
    for (int i = 0; i < count; i++) selected.add(it.next());
    LinkedHashMap<Pos, BlockValue> desired = new LinkedHashMap<>();
    for (History h : selected)
      for (Entry e : h.entries) desired.put(e.pos, forward ? e.after : e.before);
    if (desired.size() > limit)
      throw new IllegalArgumentException("Undo one step at a time or raise the block limit.");
    // The regular job journals the restoration, but completion replaces that temporary history.
    List<History> oldUndo = List.copyOf(undo), oldRedo = List.copyOf(redo);
    submit(
        desired.keySet(),
        desired::get,
        () -> {
          undo.clear();
          undo.addAll(oldUndo);
          redo.clear();
          redo.addAll(oldRedo);
          for (int i = 0; i < count; i++) {
            source.removeLast();
            target.addLast(selected.get(i));
          }
          trim();
        });
  }
}
