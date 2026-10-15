package local.luke.power.inventory;

import java.util.Objects;

/** A double click belongs to one slot, cursor and container, with a release between presses. */
public final class DoubleClick {
    public static final long INTERVAL = 250_000_000L;
    private Object owner, slot, cursor, contents;
    private long pressed;
    private boolean released;

    public void start(Object owner, Object slot, Object cursor, Object contents, long now) {
        this.owner = owner;
        this.slot = slot;
        this.cursor = cursor;
        this.contents = contents;
        pressed = now;
        released = false;
    }

    public boolean second(Object owner, Object slot, Object cursor, Object contents, long now) {
        return released && current(owner, cursor, contents, now) && this.slot == slot;
    }

    public boolean current(Object owner, Object cursor, Object contents, long now) {
        long elapsed = now - pressed;
        return owns(owner, cursor, contents) && elapsed >= 0 && elapsed <= INTERVAL;
    }

    public boolean owns(Object owner, Object cursor, Object contents) {
        return this.owner != null && this.owner == owner && this.cursor == cursor
                && Objects.equals(this.contents, contents);
    }

    public boolean pending() { return owner != null; }

    public void move(Object slot) { if (this.slot != slot) clear(); }
    public void release() { released = true; }
    public void clear() { owner = slot = cursor = contents = null; released = false; }
}
