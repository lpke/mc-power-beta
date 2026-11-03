package local.luke.power.controls.tweaks.rawinput;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;

class RawInputDiscoveryTest {
    @Test void firstDiscoveryReturnsWhileTheSlowProviderIsStillPending() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        var caller = Thread.currentThread(); var discoveredOn = new AtomicReference<Thread>();
        var discovery = new RawInputDiscovery<>(() -> {
            discoveredOn.set(Thread.currentThread()); entered.countDown();
            await(release); return List.of(7);
        }, devices -> !devices.isEmpty());
        discovery.enable();
        try {
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            assertTrue(discovery.pending()); assertNull(discovery.poll());
            assertNotSame(caller, discoveredOn.get());
            assertTrue(discoveredOn.get().isDaemon());
        } finally { release.countDown(); }
        assertEquals(List.of(7), result(discovery).devices());
        assertFalse(discovery.pending()); assertNull(discovery.poll());
    }

    @Test void disableRejectsTheOldResultAndImmediateReenableKeepsItsNewRequest() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        var calls = new AtomicInteger(); var active = new AtomicInteger(); var maximum = new AtomicInteger();
        var discovery = new RawInputDiscovery<>(() -> {
            maximum.accumulateAndGet(active.incrementAndGet(), Math::max);
            try {
                int number = calls.incrementAndGet();
                if (number == 1) { entered.countDown(); await(release); }
                return List.of(number);
            } finally { active.decrementAndGet(); }
        }, devices -> !devices.isEmpty());
        discovery.enable();
        try {
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            discovery.disable(); assertFalse(discovery.pending()); assertNull(discovery.poll());
            discovery.enable(); assertTrue(discovery.pending());
        } finally { release.countDown(); }
        assertEquals(List.of(2), result(discovery).devices());
        assertEquals(2, calls.get()); assertEquals(1, maximum.get());
    }

    @Test void successfulDiscoveryIsReusedAcrossWorldChangesAndReenableUntilAnExplicitRefresh() throws Exception {
        var calls = new AtomicInteger();
        var discovery = new RawInputDiscovery<>(() -> List.of(calls.incrementAndGet()), devices -> !devices.isEmpty());
        discovery.enable(); var first = result(discovery).devices();
        discovery.enable(); assertSame(first, result(discovery).devices());
        discovery.disable(); discovery.enable(); assertSame(first, result(discovery).devices());
        assertEquals(1, calls.get());
        discovery.refresh(); assertEquals(List.of(2), result(discovery).devices());
        assertEquals(2, calls.get());
    }

    @Test void failureAndNoDevicesAreReportedWithoutPoisoningLaterRetries() throws Exception {
        var calls = new AtomicInteger();
        var discovery = new RawInputDiscovery<List<Integer>>(() -> {
            return switch (calls.incrementAndGet()) {
                case 1 -> throw new UnsatisfiedLinkError("fixture native initialization failure");
                case 2 -> List.of();
                default -> List.of(9);
            };
        }, devices -> !devices.isEmpty());
        discovery.enable(); assertInstanceOf(UnsatisfiedLinkError.class, result(discovery).failure());
        assertFalse(discovery.pending()); assertNull(discovery.poll());
        discovery.disable(); discovery.enable(); assertEquals(List.of(), result(discovery).devices());
        discovery.disable(); discovery.enable(); assertEquals(List.of(9), result(discovery).devices());
        assertEquals(3, calls.get());
    }

    @Test void aPendingRefreshDoesNotPauseTheIndependentRawPollWorker() throws Exception {
        var enabled = new AtomicBoolean(true); var workerThread = new AtomicReference<Thread>();
        var first = new CountDownLatch(1); var continued = new CountDownLatch(4);
        var worker = new RawInputWorker<Integer>(enabled::get, devices -> { first.countDown(); continued.countDown(); }, workerThread::set);
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        var discovery = new RawInputDiscovery<>(() -> { entered.countDown(); await(release); return List.of(1); }, devices -> true);
        worker.start(); discovery.enable();
        try {
            assertTrue(first.await(2, TimeUnit.SECONDS)); assertTrue(entered.await(2, TimeUnit.SECONDS));
            assertTrue(continued.await(2, TimeUnit.SECONDS));
            assertTrue(discovery.pending()); assertTrue(workerThread.get().isAlive());
        } finally {
            release.countDown(); enabled.set(false); worker.wake(); workerThread.get().join(2000);
        }
        assertFalse(workerThread.get().isAlive()); result(discovery);
    }

    @Test void anUnsuccessfulRefreshInvalidatesThePreviousDeviceCache() throws Exception {
        var calls = new AtomicInteger();
        var discovery = new RawInputDiscovery<>(() -> {
            int number = calls.incrementAndGet();
            return number == 2 ? List.<Integer>of() : List.of(number);
        }, devices -> !devices.isEmpty());
        discovery.enable(); assertEquals(List.of(1), result(discovery).devices());
        discovery.refresh(); assertTrue(result(discovery).devices().isEmpty());
        discovery.disable(); discovery.enable();
        assertEquals(List.of(3), result(discovery).devices());
        assertEquals(3, calls.get());
    }

    private static void await(CountDownLatch latch) {
        try { if (!latch.await(2, TimeUnit.SECONDS)) throw new IllegalStateException("fixture discovery timed out"); }
        catch (InterruptedException failure) { throw new IllegalStateException(failure); }
    }
    private static <T> RawInputDiscovery.Result<T> result(RawInputDiscovery<T> discovery) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        RawInputDiscovery.Result<T> result;
        while ((result = discovery.poll()) == null && System.nanoTime() < deadline) Thread.sleep(1);
        assertNotNull(result, "discovery did not complete"); return result;
    }
}
