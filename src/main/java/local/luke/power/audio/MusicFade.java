package local.luke.power.audio;

/** Linear opening/ending envelope. Short tracks meet at the smaller of the two ramps. */
public final class MusicFade {
  private MusicFade() {}

  public static float gain(int milliseconds, double position, double duration) {
    if (milliseconds <= 0) return 1;
    double seconds = milliseconds / 1000d;
    double opening = Math.max(0, position) / seconds;
    double ending = duration > 0 ? Math.max(0, duration - position) / seconds : 1;
    return (float) Math.max(0, Math.min(1, Math.min(opening, ending)));
  }
}
