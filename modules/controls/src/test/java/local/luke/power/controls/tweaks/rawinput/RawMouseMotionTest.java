package local.luke.power.controls.tweaks.rawinput;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RawMouseMotionTest {
    @Test
    void aPollFinishingAfterDisableOrCursorGrabCannotMoveTheNewSession() throws Exception {
        var motion = new RawMouseMotion();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        Thread poll = new Thread(() -> {
            var samples = motion.capture();
            entered.countDown();
            try { release.await(); }
            catch (InterruptedException interrupted) { throw new AssertionError(interrupted); }
            samples.add(19, -11);
        });
        poll.start();
        try {
            assertTrue(entered.await(2, java.util.concurrent.TimeUnit.SECONDS));
            motion.discard();
            motion.add(2, 3);
        } finally { release.countDown(); poll.join(2000); }
        assertFalse(poll.isAlive());
        long frame = motion.drain();
        assertEquals(2, RawMouseMotion.x(frame)); assertEquals(3, RawMouseMotion.y(frame));
        assertEquals(0, motion.drain());
    }

    @Test
    void signedAxesAndDiscardKeepTheNextFrameIndependent() {
        var motion = new RawMouseMotion();
        motion.add(-8, 3);
        motion.add(2, -9);
        long sample = motion.drain();
        assertEquals(-6, RawMouseMotion.x(sample));
        assertEquals(-6, RawMouseMotion.y(sample));
        assertEquals(0, motion.drain());
        motion.add(5, 9);
        motion.clear();
        motion.add(2, -3);
        sample = motion.drain();
        assertEquals(2, RawMouseMotion.x(sample));
        assertEquals(-3, RawMouseMotion.y(sample));
    }

    @Test
    void concurrentPollingAndFramesDeliverEveryPairedCountExactlyOnce() throws Exception {
        var motion = new RawMouseMotion();
        var start = new CountDownLatch(1);
        var producers = new AtomicInteger(4);
        var threads = new Thread[4];
        int count = 100000;
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                try {
                    start.await();
                    for (int n = 0; n < count; n++) motion.add(3, -2);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    producers.decrementAndGet();
                }
            });
            threads[i].start();
        }
        start.countDown();
        long x = 0, y = 0;
        while (producers.get() > 0) {
            long sample = motion.drain();
            assertEquals(RawMouseMotion.x(sample) * 2L, -RawMouseMotion.y(sample) * 3L,
                    "a frame split a paired device sample");
            x += RawMouseMotion.x(sample);
            y += RawMouseMotion.y(sample);
        }
        for (Thread thread : threads) thread.join();
        long sample = motion.drain();
        x += RawMouseMotion.x(sample);
        y += RawMouseMotion.y(sample);
        assertEquals(4L * count * 3, x);
        assertEquals(-4L * count * 2, y);
        assertEquals(0, motion.drain());
    }
}
