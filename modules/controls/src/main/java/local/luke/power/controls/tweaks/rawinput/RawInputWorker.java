package local.luke.power.controls.tweaks.rawinput;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** One device poll at a time, including an enable racing the previous worker's exit. */
final class RawInputWorker<T> {
    private final BooleanSupplier enabled;
    private final Consumer<List<T>> poll;
    private final Consumer<Thread> publish;
    private final AtomicReference<Thread> owner = new AtomicReference<>();
    private volatile List<T> devices = List.of();

    RawInputWorker(BooleanSupplier enabled, Consumer<List<T>> poll, Consumer<Thread> publish) {
        this.enabled = enabled;
        this.poll = poll;
        this.publish = publish;
    }

    List<T> devices() { return devices; }

    void devices(Collection<? extends T> next) { devices = List.copyOf(next); }

    void start() {
        if (!enabled.getAsBoolean() || owner.get() != null) return;
        Thread next = new Thread(this::run, "inputThread");
        if (owner.compareAndSet(null, next)) {
            publish.accept(next);
            next.start();
        }
    }

    void wake() {
        Thread thread = owner.get();
        if (thread != null) thread.interrupt();
    }

    private void run() {
        boolean stopped = false;
        try {
            while (enabled.getAsBoolean()) {
                poll.accept(devices);
                try { Thread.sleep(1); }
                catch (InterruptedException stopping) { /* Recheck enable state immediately. */ }
            }
            stopped = true;
        } finally {
            owner.compareAndSet(Thread.currentThread(), null);
            // Start may have observed this owner after its final enabled check.
            // Poll failures retain native uncaught-error behavior, without a restart loop.
            if (stopped && enabled.getAsBoolean()) start();
        }
    }
}
