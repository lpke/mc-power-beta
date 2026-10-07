package local.luke.power.slabplacement.config;

public final class Settings {
  public boolean enabled = true;
  public boolean announceToggle;

  public Settings copy() {
    Settings s = new Settings();
    s.enabled = enabled;
    s.announceToggle = announceToggle;
    return s;
  }
}
