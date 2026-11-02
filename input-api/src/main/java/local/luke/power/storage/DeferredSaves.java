package local.luke.power.storage;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/** A single writer coalesces gameplay snapshots without holding the disk lock on input. */
final class DeferredSaves {
  interface Writer { void write(Map<String, Change> changes) throws IOException; }

  /** Ordered field edits compose with full snapshots without reading settings on input. */
  record Change(JsonObject value, boolean patch) {
    JsonObject apply(JsonObject previous) {
      JsonObject result = patch ? previous.deepCopy() : new JsonObject();
      value.entrySet().forEach(entry -> result.add(entry.getKey(), entry.getValue().deepCopy()));
      return result;
    }
    Change then(Change newer) {
      return newer.patch ? new Change(newer.apply(value), patch) : newer;
    }
  }

  private final Writer writer;
  private final java.util.concurrent.ExecutorService executor = Executors.newSingleThreadExecutor(job -> {
    Thread thread = new Thread(job, "Power Beta settings");
    thread.setDaemon(true);
    return thread;
  });
  private final Map<String, Change> pending = new LinkedHashMap<>();
  private boolean running;
  private IOException failure, notification;

  DeferredSaves(Writer writer) { this.writer = writer; }

  synchronized void submit(String id, JsonObject value) {
    pending.put(id, new Change(value.deepCopy(), false));
    start();
  }

  synchronized void patch(String id, JsonObject fields) {
    pending.merge(id, new Change(fields.deepCopy(), true), Change::then);
    start();
  }

  private void start() {
    if (running || pending.isEmpty()) return;
    running = true;
    executor.execute(this::drain);
  }

  private void drain() {
    while (true) {
      Map<String, Change> batch;
      synchronized (this) {
        if (pending.isEmpty()) {
          running = false;
          failure = null;
          notifyAll();
          return;
        }
        batch = new LinkedHashMap<>(pending);
        pending.clear();
      }
      try {
        writer.write(batch);
      } catch (Exception e) {
        synchronized (this) {
          // Keep unsaved values for a later toggle or explicit flush. Newer edits win.
          batch.forEach((id, older) -> pending.merge(id, older, (newer, failed) -> failed.then(newer)));
          failure = e instanceof IOException io ? io : new IOException("Cannot save gameplay settings", e);
          notification = failure;
          running = false;
          notifyAll();
        }
        return;
      }
    }
  }

  synchronized void flush() throws IOException {
    start();
    while (running) {
      try { wait(); }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("Interrupted while saving settings", e);
      }
    }
    if (!pending.isEmpty()) throw failure;
  }

  synchronized IOException takeFailure() {
    IOException result = notification;
    notification = null;
    return result;
  }
}
