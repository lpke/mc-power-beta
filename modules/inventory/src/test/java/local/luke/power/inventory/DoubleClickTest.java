package local.luke.power.inventory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DoubleClickTest {
    private final DoubleClick clicks = new DoubleClick();
    private final Object container = new Object(), slot = new Object(), cursor = new Object();

    @Test void requiresReleaseAndASecondPressWithinRealTime() {
        clicks.start(container, slot, cursor, "item/count/data", 100);
        assertFalse(clicks.second(container, slot, cursor, "item/count/data", 101));
        clicks.release();
        assertTrue(clicks.second(container, slot, cursor, "item/count/data", 100 + DoubleClick.INTERVAL));
        assertFalse(clicks.second(container, slot, cursor, "item/count/data", 101 + DoubleClick.INTERVAL));
        assertFalse(clicks.second(container, slot, cursor, "item/count/data", 99));
    }

    @Test void changedCursorContentsIdentityOrContainerCancelTheGesture() {
        clicks.start(container, slot, cursor, "item/count/data", 100);
        clicks.release();
        assertFalse(clicks.second(container, slot, cursor, "item/changed/data", 101));
        assertFalse(clicks.second(container, slot, new Object(), "item/count/data", 101));
        assertFalse(clicks.second(new Object(), slot, cursor, "item/count/data", 101));
        assertFalse(clicks.second(container, new Object(), cursor, "item/count/data", 101));
    }

    @Test void dragOrFocusCancellationCannotRestartOnRelease() {
        clicks.start(container, slot, cursor, "data", 100);
        clicks.move(new Object());
        clicks.move(slot);
        clicks.release();
        assertFalse(clicks.second(container, slot, cursor, "data", 101));
        clicks.start(container, slot, cursor, "data", 100);
        clicks.clear();
        clicks.release();
        assertFalse(clicks.second(container, slot, cursor, "data", 101));
    }

    @Test void aFreshGestureDoesNotInheritThePreviousRelease() {
        clicks.start(container, slot, cursor, "first", 100);
        clicks.release();
        clicks.start(container, slot, cursor, "second", 110);
        assertFalse(clicks.second(container, slot, cursor, "second", 111));
        clicks.release();
        assertTrue(clicks.second(container, slot, cursor, "second", 111));
    }

    @Test void monotonicClockMayWrapWithoutExtendingTheInterval() {
        clicks.start(container, slot, cursor, "data", Long.MAX_VALUE - 1);
        clicks.release();
        assertTrue(clicks.second(container, slot, cursor, "data", Long.MIN_VALUE + 1));
    }

    @Test void slowFramesDoNotChangeTheIntervalBetweenQueuedEvents() {
        long first = 1_000_000_000L;
        clicks.start(container, slot, cursor, "data", first);
        clicks.release();
        // A render ownership check cannot discard the queued second event based on processing time.
        assertTrue(clicks.owns(container, cursor, "data"));
        assertTrue(clicks.second(container, slot, cursor, "data", first + 100_000_000L));
        assertFalse(clicks.second(container, slot, cursor, "data", first + 300_000_000L));
        assertFalse(clicks.owns(container, cursor, "changed"));
    }
}
