package local.luke.power.audio;

/** Monotonic play time excludes pauses and asynchronous stream startup. */
public final class MusicProgress {
  private double seconds;
  private long last;
  private boolean advancing;

  public void reset(double offset, long now) {
    seconds = Math.max(0, offset);
    last = now;
    advancing = false;
  }

  public void update(boolean playing, long now) {
    if (advancing && playing) seconds += Math.max(0, now - last) / 1_000_000_000d;
    last = now;
    advancing = playing;
  }

  public double seconds() {
    return seconds;
  }
}
