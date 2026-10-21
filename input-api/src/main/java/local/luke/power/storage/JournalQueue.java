package local.luke.power.storage;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.*;
import java.io.IOException;

/** One bounded writer. Neither queued work nor cancellation can expose mutable game objects. */
public final class JournalQueue {
  private static final ThreadPoolExecutor IO = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
      new ArrayBlockingQueue<>(4), task -> {
        Thread thread = new Thread(task, "Power Beta inventory journal");
        thread.setDaemon(true); return thread;
      }, new ThreadPoolExecutor.AbortPolicy());

  private JournalQueue() {}

  /** Explicit menu/shutdown barrier. Callers must never wait from input, render or the writer itself. */
  public static void awaitQueued() throws IOException {
    if (Thread.currentThread().getName().equals("Power Beta inventory journal"))
      throw new IOException("The inventory writer cannot wait for itself");
    if (IO.getPoolSize() == 0 && IO.getQueue().isEmpty()) return;
    var barrier = new FutureTask<Void>(() -> null);
    try {
      try { IO.execute(barrier); }
      catch (RejectedExecutionException busy) {
        if (IO.isShutdown() || !IO.getQueue().offer(barrier, 30, TimeUnit.SECONDS))
          throw new IOException("Inventory journal is still busy");
      }
      barrier.get(30, TimeUnit.SECONDS);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt(); throw new IOException("Interrupted while flushing inventory recovery", interrupted);
    } catch (ExecutionException | TimeoutException failure) { throw new IOException("Inventory recovery flush did not finish", failure); }
  }

  public static Future<?> append(AppendJournal journal, byte[] bytes) {
    Objects.requireNonNull(journal);
    if (bytes == null || bytes.length == 0 || bytes.length > AppendJournal.MAX_RECORD)
      throw new IllegalArgumentException("Invalid inventory journal payload");
    byte[] immutable = Arrays.copyOf(bytes, bytes.length);
    // Cancellation belongs to publication. An append already admitted must finish its entire record.
    CompletableFuture<Void> durable = new CompletableFuture<>();
    IO.execute(() -> {
      try { journal.append(immutable); durable.complete(null); }
      catch (Throwable failure) { durable.completeExceptionally(failure); }
    });
    return durable;
  }
}
