package local.luke.power.controls.tweaks.rawinput;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;

class RawInputWorkerTest {
    @Test void disabledStartDoesNoWorkAndStopTerminatesThePollingWorker() throws Exception {
        var enabled = new AtomicBoolean();
        var thread = new AtomicReference<Thread>();
        var polled = new CountDownLatch(1);
        var worker = new RawInputWorker<Integer>(enabled::get, devices -> polled.countDown(), thread::set);
        worker.start();
        assertNull(thread.get()); assertEquals(1, polled.getCount());
        enabled.set(true); worker.start();
        try { assertTrue(polled.await(2, TimeUnit.SECONDS)); }
        finally { stop(enabled, worker, thread); }
        Thread stopped = thread.get(); worker.start();
        assertSame(stopped, thread.get()); assertFalse(stopped.isAlive());
    }

    @Test void immediateOffOnDuringAPollDoesNotLoseTheWorkerOrOverlapDevices() throws Exception {
        var enabled = new AtomicBoolean(true);
        var thread = new AtomicReference<Thread>();
        var entered = new CountDownLatch(1); var release = new Semaphore(0); var resumed = new CountDownLatch(1);
        var active = new AtomicInteger(); var maximum = new AtomicInteger(); var calls = new AtomicInteger();
        var worker = new RawInputWorker<Integer>(enabled::get, devices -> {
            maximum.accumulateAndGet(active.incrementAndGet(), Math::max);
            try {
                if (calls.incrementAndGet() == 1) { entered.countDown(); release.acquireUninterruptibly(); }
                else resumed.countDown();
            } finally { active.decrementAndGet(); }
        }, thread::set);
        worker.start();
        try {
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            Thread first = thread.get();
            enabled.set(false); worker.wake(); enabled.set(true); worker.start();
            assertSame(first, thread.get());
            release.release();
            assertTrue(resumed.await(2, TimeUnit.SECONDS)); assertEquals(1, maximum.get());
        } finally { release.release(); stop(enabled, worker, thread); }
    }

    @Test void enablingAfterTheOldExitDecisionStartsAReplacementWithoutLosingTheRequest() throws Exception {
        var enabled = new AtomicBoolean(true); var holdExit = new AtomicBoolean(true);
        var thread = new AtomicReference<Thread>(); var firstPoll = new CountDownLatch(1);
        var exiting = new CountDownLatch(1); var release = new Semaphore(0); var restarted = new CountDownLatch(1);
        BooleanSupplier running = () -> {
            boolean value = enabled.get();
            if (!value && Thread.currentThread() == thread.get() && holdExit.compareAndSet(true, false)) {
                exiting.countDown(); release.acquireUninterruptibly();
            }
            return value;
        };
        var worker = new RawInputWorker<Integer>(running, devices -> firstPoll.countDown(), next -> {
            if (thread.getAndSet(next) != null) restarted.countDown();
        });
        worker.start();
        try {
            assertTrue(firstPoll.await(2, TimeUnit.SECONDS));
            Thread old = thread.get(); enabled.set(false); worker.wake();
            assertTrue(exiting.await(2, TimeUnit.SECONDS));
            enabled.set(true); worker.start();
            assertSame(old, thread.get(), "old poll owner must finish before a replacement starts");
            release.release();
            assertTrue(restarted.await(2, TimeUnit.SECONDS));
            old.join(2000); assertFalse(old.isAlive()); assertNotSame(old, thread.get());
        } finally { release.release(); stop(enabled, worker, thread); }
    }

    @Test void concurrentDiscoveryPublishesOnlyCompleteImmutableDeviceLists() throws Exception {
        var enabled = new AtomicBoolean(true); var thread = new AtomicReference<Thread>();
        var failure = new AtomicReference<Throwable>(); var last = new CountDownLatch(1);
        var worker = new RawInputWorker<Integer>(enabled::get, devices -> {
            try {
                assertEquals(2, devices.size()); assertEquals(devices.get(0), devices.get(1));
                if (devices.get(0) == 10000) last.countDown();
            } catch (Throwable problem) { failure.compareAndSet(null, problem); }
        }, thread::set);
        var discovered = new ArrayList<>(List.of(0, 0)); worker.devices(discovered);
        List<Integer> first = worker.devices(); discovered.clear();
        assertEquals(List.of(0, 0), first);
        assertThrows(UnsupportedOperationException.class, () -> first.add(1));
        worker.start();
        try {
            for (int i = 1; i <= 10000; i++) worker.devices(List.of(i, i));
            assertTrue(last.await(2, TimeUnit.SECONDS)); assertNull(failure.get());
            assertEquals(List.of(0, 0), first);
        } finally { stop(enabled, worker, thread); }
    }

    @Test void hardwarePollFailureDoesNotCreateARestartLoop() throws Exception {
        var enabled = new AtomicBoolean(true); var thread = new AtomicReference<Thread>();
        var failure = new AtomicReference<Throwable>(); var calls = new AtomicInteger();
        var worker = new RawInputWorker<Integer>(enabled::get, devices -> {
            calls.incrementAndGet(); throw new IllegalStateException("fixture device failure");
        }, next -> { thread.set(next); next.setUncaughtExceptionHandler((owner, problem) -> failure.set(problem)); });
        worker.start(); thread.get().join(2000);
        try {
            assertFalse(thread.get().isAlive()); assertEquals(1, calls.get());
            assertInstanceOf(IllegalStateException.class, failure.get());
        } finally { stop(enabled, worker, thread); }
    }

    private static void stop(AtomicBoolean enabled, RawInputWorker<?> worker, AtomicReference<Thread> thread)
            throws InterruptedException {
        enabled.set(false); worker.wake();
        Thread stopped = thread.get();
        if (stopped != null) { stopped.join(2000); assertFalse(stopped.isAlive(), "OFF left a polling worker alive"); }
    }
}
