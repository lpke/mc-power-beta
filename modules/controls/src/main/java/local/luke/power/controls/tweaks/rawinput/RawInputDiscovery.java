package local.luke.power.controls.tweaks.rawinput;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Slow device discovery never occupies the game thread or the active device-poll worker. */
final class RawInputDiscovery<T> {
    record Result<T>(long generation, T devices, Throwable failure) {}
    private record Request(long generation) {}
    private final Supplier<T> discover;
    private final Predicate<T> cacheable;
    private final AtomicReference<Thread> owner = new AtomicReference<>();
    private final AtomicReference<Request> requested = new AtomicReference<>();
    private final AtomicReference<Result<T>> completed = new AtomicReference<>();
    // Requests, cache and acceptance belong to the game thread.
    private long generation;
    private boolean enabled, pending;
    private T cached;

    RawInputDiscovery(Supplier<T> discover, Predicate<T> cacheable) {
        this.discover = discover;
        this.cacheable = cacheable;
    }

    void enable() { if (!enabled || !pending) request(false); }
    void refresh() { if (enabled) request(true); }
    boolean pending() { return pending; }

    void disable() {
        enabled = pending = false;
        generation++;
        requested.set(null);
        completed.set(null);
    }

    private void request(boolean refresh) {
        enabled = pending = true;
        long next = ++generation;
        if (!refresh && cached != null) {
            requested.set(null);
            publish(new Result<>(next, cached, null));
        } else {
            requested.set(new Request(next));
            start();
        }
    }

    Result<T> poll() {
        Result<T> result = completed.getAndSet(null);
        if (result == null || !enabled || result.generation != generation) return null;
        pending = false;
        cached = result.failure == null && cacheable.test(result.devices) ? result.devices : null;
        return result;
    }

    private void publish(Result<T> result) {
        completed.accumulateAndGet(result,
                (before, after) -> before != null && before.generation > after.generation ? before : after);
    }

    private void start() {
        if (owner.get() != null) return;
        Thread next = new Thread(this::run, "raw-input-discovery");
        next.setDaemon(true);
        if (owner.compareAndSet(null, next)) next.start();
    }

    private void run() {
        try {
            Request request;
            while ((request = requested.getAndSet(null)) != null) {
                try { publish(new Result<>(request.generation, discover.get(), null)); }
                catch (RuntimeException | LinkageError failure) { publish(new Result<>(request.generation, null, failure)); }
            }
        } finally {
            owner.compareAndSet(Thread.currentThread(), null);
            if (requested.get() != null) start();
        }
    }
}
