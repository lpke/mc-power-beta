package local.luke.power.creative.flight;

/** A flight-local latch. Focus, mode and flight changes require a fresh press. */
public final class FlightSprint {
  private boolean down, latched, toggleMode;

  public void reset(boolean pressed) {
    down = pressed;
    latched = false;
  }

  public boolean update(boolean pressed, boolean eligible, boolean toggle) {
    if (!eligible || toggleMode != toggle) latched = false;
    if (eligible && toggle && pressed && !down) latched = !latched;
    down = pressed;
    toggleMode = toggle;
    return eligible && (toggle ? latched : pressed);
  }
}
