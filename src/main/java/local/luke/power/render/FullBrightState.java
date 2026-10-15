package local.luke.power.render;

/** A spectator visit has its own override; leaving restores the manual preference. */
public final class FullBrightState {
  private Object world;
  private boolean manual, spectator, automatic;
  private Boolean override;
  public void update(Object world, boolean spectatorAllowed, boolean spectator, boolean automatic) {
    if (this.world != world || world == null) { manual = false; override = null; this.spectator = false; }
    this.world = world;
    spectator &= spectatorAllowed;
    if (this.spectator != spectator || this.automatic != automatic) override = null;
    this.spectator = spectator;
    this.automatic = automatic;
  }
  public boolean active() { return override != null ? override : manual || spectator && automatic; }
  public void toggle() {
    if (spectator && automatic) override = !active();
    else { manual = !manual; override = null; }
  }
}
