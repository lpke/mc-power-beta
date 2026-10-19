package local.luke.power.map;

import java.util.concurrent.*;

/** A shared bounded worker never reads native world/entity/GL objects. */
public final class MapIO {
  private static final java.util.concurrent.atomic.AtomicLong SUBMITTED = new java.util.concurrent.atomic.AtomicLong();
  private static final ConcurrentMap<CompletableFuture<?>, Runnable> QUEUED = new ConcurrentHashMap<>();
  private static final ThreadPoolExecutor WORKER =
      new ThreadPoolExecutor(
          1,
          1,
          0,
          TimeUnit.SECONDS,
          new ArrayBlockingQueue<>(64),
          task -> {
            Thread thread = new Thread(task, "Power Beta map storage");
            thread.setDaemon(true);
            return thread;
          },
          new ThreadPoolExecutor.AbortPolicy());

  private MapIO() {}

  public static <T> CompletableFuture<T> submit(Callable<T> task) {
    var result = new CompletableFuture<T>();
    Runnable queued = () -> {
      try { result.complete(task.call()); }
      catch (Exception e) { result.completeExceptionally(e); }
      finally { QUEUED.remove(result); }
    };
    QUEUED.put(result, queued);
    try {
      WORKER.execute(queued);
      SUBMITTED.incrementAndGet();
    } catch (RejectedExecutionException e) {
      QUEUED.remove(result);
      return null;
    }
    return result;
  }

  /** Only a caller that owns an obsolete view read may cancel it, before it starts. */
  static boolean cancelQueuedRead(CompletableFuture<?> result) {
    Runnable queued = QUEUED.get(result);
    if (queued == null || !WORKER.getQueue().remove(queued)) return false;
    QUEUED.remove(result, queued);
    result.cancel(false);
    return true;
  }

  /** Exit-only barrier: waits for already queued work even when the queue is saturated. */
  public static void awaitQueued() {
    CompletableFuture<Void> barrier = submit(() -> null);
    if (barrier == null) {
      barrier = new CompletableFuture<>();
      var completion = barrier;
      try {
        WORKER.getQueue().put(() -> completion.complete(null));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new java.util.concurrent.CompletionException(e);
      }
    }
    barrier.join();
  }

  public static long submitted() { return SUBMITTED.get(); }

  public static int queued() {
    return WORKER.getQueue().size();
  }
}
