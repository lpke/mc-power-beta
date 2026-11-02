package local.luke.power.controls.tweaks.rawinput;

import java.util.concurrent.atomic.AtomicLong;

/** Transfers a pair of relative mouse counts from the polling thread to one frame. */
public final class RawMouseMotion {
    private volatile Samples pending = new Samples();

    public void add(int x, int y) {
        pending.add(x, y);
    }

    public long drain() {
        return pending.counts.getAndSet(0);
    }

    public void clear() {
        pending.counts.set(0);
    }

    /** Capture before device polling, so a late poll cannot cross an input-session boundary. */
    public Samples capture() { return pending; }

    public void discard() { pending = new Samples(); }

    public static final class Samples {
        private final AtomicLong counts = new AtomicLong();

        public void add(int x, int y) {
            if (x == 0 && y == 0) return;
            long before, after;
            do {
                before = counts.get();
                after = pack(x(before) + x, y(before) + y);
            } while (!counts.compareAndSet(before, after));
        }
    }

    public static int x(long sample) {
        return (int) (sample >> 32);
    }

    public static int y(long sample) {
        return (int) sample;
    }

    private static long pack(int x, int y) {
        return (long) x << 32 | y & 0xffffffffL;
    }
}
