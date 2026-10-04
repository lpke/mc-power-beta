package local.luke.power.audio;

/** Delay policy shared by automatic playback and the status countdown. */
public final class MusicTiming {
  private MusicTiming() {}

  public static int remaining(boolean wait, boolean queueDelay, boolean queued, int countdown) {
    return wait && (!queued || queueDelay) ? Math.max(0, countdown) : 0;
  }

  public static String status(int ticks) {
    long seconds = (Math.max(0L, ticks) + 19) / 20;
    if (seconds == 0) return "Not playing. Next track shortly.";
    return "Not playing. " + seconds + " sec until next track.";
  }
}
