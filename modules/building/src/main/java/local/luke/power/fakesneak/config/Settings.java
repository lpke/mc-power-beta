package local.luke.power.fakesneak.config;

public final class Settings {
  public boolean enabled = false;
  public boolean announceToggle;

  public Settings copy() {
    Settings s = new Settings();
    s.enabled = enabled;
    s.announceToggle = announceToggle;
    return s;
  }
}
