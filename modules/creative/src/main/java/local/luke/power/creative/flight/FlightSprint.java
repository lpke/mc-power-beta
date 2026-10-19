package local.luke.power.creative.flight;

/** A flight-local latch. Actual takeoff can inherit ground sprint once; resets discard it. */
public final class FlightSprint {
  private boolean down, latched, toggleMode, takeoff;

  public void reset(boolean pressed) {
    down = pressed;
    latched = false;
    takeoff = false;
  }

  /** Remember the originating press so an already-held key cannot switch Toggle off. */
  public void takeoff(boolean groundSprint, boolean pressed) {
    reset(pressed);
    takeoff = groundSprint;
  }

  public boolean update(boolean pressed, boolean eligible, boolean toggle) {
    boolean inherit = takeoff;
    takeoff = false;
    if (!eligible || toggleMode != toggle) latched = false;
    if (eligible && toggle) {
      if (inherit) latched = true;
      if (pressed && !down) latched = !latched;
    }
    down = pressed;
    toggleMode = toggle;
    return eligible && (toggle ? latched : pressed);
  }
}
