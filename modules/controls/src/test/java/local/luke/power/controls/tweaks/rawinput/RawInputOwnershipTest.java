package local.luke.power.controls.tweaks.rawinput;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class RawInputOwnershipTest {
    @Test void onlyAnOwnedWorldAndPlayerCanPublishSamples() {
        var motion = new RawMouseMotion();
        var ownership = new RawInputOwnership(motion);
        Object world = new Object(), player = new Object();
        assertFalse(ownership.update(false, world, player));
        assertFalse(ownership.update(true, null, player));
        assertFalse(ownership.update(true, world, null));
        assertNull(ownership.destination());
        assertTrue(ownership.update(true, world, player));
        var current = ownership.destination();
        current.add(7, -5);
        assertTrue(ownership.update(true, world, player));
        assertSame(current, ownership.destination(), "steady frames must not reset or allocate a new session");
        assertEquals(7, RawMouseMotion.x(motion.drain()));
        assertFalse(ownership.update(false, world, player));
        assertNull(ownership.destination());
        current.add(100, 200);
        assertEquals(0, motion.drain(), "a retired poll reached the render accumulator");
        var inactive = motion.capture();
        ownership.update(false, world, player);
        assertSame(inactive, motion.capture(), "unfocused/disabled frames must not keep allocating sessions");
    }

    @Test void releaseAndRegrabBetweenFramesRetireAPollAlreadyInFlight() throws Exception {
        var motion = new RawMouseMotion();
        var ownership = new RawInputOwnership(motion);
        Object world = new Object(), player = new Object(), devices = new Object();
        ownership.update(true, world, player);
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        Thread poll = new Thread(() -> {
            var old = ownership.destination();
            entered.countDown();
            try { release.await(); }
            catch (InterruptedException failure) { throw new AssertionError(failure); }
            old.add(1000, -2000);
            ownership.drained(old, devices);
        });
        poll.start();
        try {
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            ownership.suspend();
            ownership.update(true, world, player);
            ownership.destination().add(3, -4);
        } finally { release.countDown(); poll.join(2000); }
        assertFalse(poll.isAlive());
        long frame = motion.drain();
        assertEquals(3, RawMouseMotion.x(frame));
        assertEquals(-4, RawMouseMotion.y(frame));
        assertFalse(ownership.ready(ownership.destination(), devices), "late old poll armed the new session");
    }

    @Test void eachNewOwnerAndDeviceListDrainsBackgroundEventsBeforeCapturing() {
        var motion = new RawMouseMotion();
        var ownership = new RawInputOwnership(motion);
        Object world = new Object(), player = new Object(), devices = new Object();
        ownership.update(true, world, player);
        var session = ownership.destination();
        assertFalse(ownership.ready(session, devices));
        ownership.drained(session, devices);
        assertTrue(ownership.ready(session, devices));
        Object replacementDevices = new Object();
        assertFalse(ownership.ready(session, replacementDevices));
        ownership.drained(session, replacementDevices);
        assertTrue(ownership.ready(session, replacementDevices));
        for (int boundary = 0; boundary < 4; boundary++) {
            var old = ownership.destination();
            old.add(500, -500);
            if (boundary == 0) { ownership.suspend(); ownership.update(true, world, player); }
            if (boundary == 1) { ownership.update(false, world, player); ownership.update(true, world, player); }
            if (boundary == 2) { world = new Object(); ownership.update(true, world, player); }
            if (boundary == 3) { player = new Object(); ownership.update(true, world, player); }
            assertNotSame(old, ownership.destination());
            assertFalse(ownership.ready(ownership.destination(), replacementDevices));
            assertEquals(0, motion.drain(), "background counts survived ownership boundary " + boundary);
            ownership.drained(ownership.destination(), replacementDevices);
            assertTrue(ownership.ready(ownership.destination(), replacementDevices));
        }
    }

    @Test void aFocusOutInBatchCannotReuseThePreviousPollOrPendingCounts() {
        var motion = new RawMouseMotion();
        var ownership = new RawInputOwnership(motion);
        Object world = new Object(), player = new Object(), devices = new Object();
        ownership.update(true, world, player);
        var before = ownership.destination();
        ownership.drained(before, devices);
        before.add(29, -31);
        ownership.suspend(); // FocusOut is observed even if no frame was rendered while unfocused.
        ownership.update(true, world, player);
        before.add(37, -41);
        var after = ownership.destination();
        assertFalse(ownership.ready(after, devices));
        assertEquals(0, motion.drain());
        ownership.drained(after, devices);
        after.add(2, 5);
        long frame = motion.drain();
        assertEquals(2, RawMouseMotion.x(frame)); assertEquals(5, RawMouseMotion.y(frame));
    }
}
