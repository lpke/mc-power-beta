package local.luke.power.audio;

/** Delay policy shared by automatic playback and the status countdown. */
public final class MusicTiming {
  private MusicTiming() {}

  public static int remaining(boolean wait, boolean queueDelay, boolean queued, int countdown) {
    return wait && (!queued || queueDelay) ? Math.max(0, countdown) : 0;
  }

  public static String status(int ticks) {
    return status(ticks, "next track");
  }

  public static String status(int ticks, String track) {
    long seconds = (Math.max(0L, ticks) + 19) / 20;
    String time = seconds <= 30 ? "<1 minute" : seconds < 90 ? "1 minute"
        : Math.round(seconds / 60d) + " minutes";
    return "Not playing. " + time + " until " + track;
  }
}
