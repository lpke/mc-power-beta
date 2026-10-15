package local.luke.power.rules;

public final class SleepThreshold {
  private SleepThreshold() {}
  public static boolean reached(int eligible, int sleeping, int percentage) {
    if (eligible <= 0 || sleeping <= 0 || percentage < 0 || percentage > 100) return false;
    return (long)sleeping * 100 >= (long)eligible * percentage;
  }
}
