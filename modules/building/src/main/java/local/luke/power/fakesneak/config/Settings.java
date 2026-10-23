package local.luke.power.fakesneak.config;

public final class Settings {
  /** Visit-local action state, deliberately excluded from persisted preferences. */
  public transient boolean enabled;
  public boolean announceToggle;

  public Settings copy() {
    Settings s = new Settings();
    s.enabled = enabled;
    s.announceToggle = announceToggle;
    return s;
  }
}
