package local.luke.power.storage;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/** A single writer coalesces gameplay snapshots without holding the disk lock on input. */
final class DeferredSaves {
  interface Writer { void write(Map<String, JsonObject> changes) throws IOException; }

  private final Writer writer;
  private final java.util.concurrent.ExecutorService executor = Executors.newSingleThreadExecutor(job -> {
    Thread thread = new Thread(job, "Power Beta settings");
    thread.setDaemon(true);
    return thread;
  });
  private final Map<String, JsonObject> pending = new LinkedHashMap<>();
  private boolean running;
  private IOException failure, notification;

  DeferredSaves(Writer writer) { this.writer = writer; }

  synchronized void submit(String id, JsonObject value) {
    pending.put(id, value.deepCopy());
    start();
  }

  private void start() {
    if (running || pending.isEmpty()) return;
    running = true;
    executor.execute(this::drain);
  }

  private void drain() {
    while (true) {
      Map<String, JsonObject> batch;
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
          batch.forEach(pending::putIfAbsent);
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
