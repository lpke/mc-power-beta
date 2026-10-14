package local.luke.power.creative;

/** One default F3 press. Cancellation never changes the current debug display. */
final class DebugKeyGesture {
  private boolean pending, used;

  void press(boolean cycleHeld) {
    if (pending) return;
    pending = true;
    used = cycleHeld;
  }

  void cycle() { if (pending) used = true; }
  boolean pending() { return pending; }

  boolean release() {
    boolean toggle = pending && !used;
    cancel();
    return toggle;
  }

  void cancel() { pending = used = false; }
}
