package local.luke.power.controls.tweaks.rawinput;

/** Main-thread ownership publishes a destination; device polls only read that publication. */
final class RawInputOwnership {
    private final RawMouseMotion motion;
    private volatile RawMouseMotion.Samples destination;
    private Object world, player;
    // Accessed only by the single device-poll owner.
    private RawMouseMotion.Samples drained;
    private Object drainedDevices;

    RawInputOwnership(RawMouseMotion motion) { this.motion = motion; }

    boolean update(boolean ownsPointer, Object world, Object player) {
        if (!ownsPointer || world == null || player == null) {
            suspend();
            return false;
        }
        if (destination == null || this.world != world || this.player != player) {
            suspend();
            motion.discard();
            this.world = world;
            this.player = player;
            destination = motion.capture();
        }
        return true;
    }

    void suspend() {
        if (destination == null) return;
        destination = null;
        motion.discard();
        world = player = null;
    }

    RawMouseMotion.Samples destination() { return destination; }

    /** The first poll drains device events left over from before ownership began. */
    boolean ready(RawMouseMotion.Samples session, Object devices) {
        return session != null && session == drained && devices == drainedDevices;
    }
    void drained(RawMouseMotion.Samples session, Object devices) { drained = session; drainedDevices = devices; }
}
