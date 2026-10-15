package local.luke.power.inventory;

/** Mouse event time avoids stretching a gesture when a frame stalls between clicks. */
public final class MouseClickClock {
    private static long event;
    private MouseClickClock() {}
    public static long now() { return event == 0 ? System.nanoTime() : event; }
    public static long event(long timestamp) { long previous = event; event = timestamp; return previous; }
}
